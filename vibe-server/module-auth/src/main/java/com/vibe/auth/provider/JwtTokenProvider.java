package com.vibe.auth.provider;

import com.vibe.auth.domain.enums.UserType;
import com.vibe.common.constant.CommonConstant;
import com.vibe.common.context.UserContext;
import com.vibe.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token 签发/解析/校验提供者
 *
 * <p>负责业务语义层面的 Token 生命周期管理：</p>
 * <ul>
 *   <li>签发：基于 {@link UserContext} 生成 Token</li>
 *   <li>解析：从 Token 还原 {@link UserContext}</li>
 *   <li>校验：是否在黑名单、是否过期</li>
 *   <li>续期：剩余有效期 &lt; 2h 时自动续签</li>
 * </ul>
 *
 * <p>多类型用户认证（Task 5）：</p>
 * <ul>
 *   <li>Token 载荷新增 {@code userType} 字段（INTERNAL/AGENT/CUSTOMER）</li>
 *   <li>不同 userType 配置不同有效期：INTERNAL 8h / AGENT 7d / CUSTOMER 2h</li>
 *   <li>有效期可通过 {@code vibe.jwt.token-validity.*} 配置覆盖</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtUtils jwtUtils;

    /** 内部用户 Token 有效期（秒），默认 8h */
    @Value("${vibe.jwt.token-validity.internal:28800}")
    private long internalTtl;

    /** 代理商 Token 有效期（秒），默认 7d */
    @Value("${vibe.jwt.token-validity.agent:604800}")
    private long agentTtl;

    /** 客户 Token 有效期（秒），默认 2h */
    @Value("${vibe.jwt.token-validity.customer:7200}")
    private long customerTtl;

    /**
     * 签发 Token（向后兼容：从 UserContext.tenantType 推导 userType）
     *
     * @param ctx         用户上下文
     * @param clientType  客户端类型：PC / MOBILE / AGENT / CUSTOMER
     * @return JWT Token 字符串
     */
    public String generateToken(UserContext ctx, String clientType) {
        return generateToken(ctx, clientType, resolveUserTypeFromContext(ctx));
    }

    /**
     * 签发 Token（显式指定 userType）
     *
     * @param ctx         用户上下文
     * @param clientType  客户端类型：PC / MOBILE / AGENT / CUSTOMER
     * @param userType    用户类型：INTERNAL / AGENT / CUSTOMER
     * @return JWT Token 字符串
     */
    public String generateToken(UserContext ctx, String clientType, UserType userType) {
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        long ttl = resolveTtl(userType, clientType);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", ctx.getUserId());
        claims.put("userName", ctx.getUserName());
        claims.put("realName", ctx.getRealName());
        claims.put("roles", ctx.getRoles());
        claims.put("tenantType", ctx.getTenantType());
        claims.put("tenantId", ctx.getTenantId());
        claims.put("orgId", ctx.getOrgId());
        claims.put("clientType", clientType);
        // Task 5: Token 载荷新增 userType 字段
        claims.put("userType", userType == null ? UserType.INTERNAL.name() : userType.name());
        ctx.setTokenId(tokenId);
        ctx.setClientType(clientType);

        return jwtUtils.generateToken(claims, tokenId, ttl);
    }

    /**
     * 从 Token 解析出用户上下文
     */
    public UserContext parseUserContext(String token) {
        Claims claims = jwtUtils.parseToken(token);
        return UserContext.builder()
                .userId(jwtUtils.getUserId(claims))
                .userName(jwtUtils.getUserName(claims))
                .realName(jwtUtils.getRealName(claims))
                .roles(jwtUtils.getRoles(claims))
                .tenantType(jwtUtils.getTenantType(claims))
                .tenantId(jwtUtils.getTenantId(claims))
                .orgId(jwtUtils.getOrgId(claims))
                .clientType(jwtUtils.getClientType(claims))
                .tokenId(jwtUtils.getTokenId(claims))
                .build();
    }

    /**
     * Task 5：从 Token 解析 userType
     *
     * @param token JWT Token
     * @return UserType 实例，缺失时返回 null
     */
    public UserType parseUserType(String token) {
        Claims claims = jwtUtils.parseToken(token);
        Object v = claims.get("userType");
        if (v == null) {
            return null;
        }
        return UserType.fromString(v.toString());
    }

    /**
     * 校验 Token 是否有效（含黑名单校验，业务层补充）
     */
    public boolean validateToken(String token) {
        return jwtUtils.isValid(token);
    }

    /**
     * 判断是否需要续签
     */
    public boolean needsRenew(String token) {
        return jwtUtils.needsRenew(token, CommonConstant.TOKEN_RENEW_THRESHOLD);
    }

    /**
     * 续签 Token（生成新 Token，载荷保持一致，但 jti 与过期时间更新）
     */
    public String renewToken(String oldToken) {
        Claims claims = jwtUtils.parseToken(oldToken);
        UserContext ctx = UserContext.builder()
                .userId(jwtUtils.getUserId(claims))
                .userName(jwtUtils.getUserName(claims))
                .realName(jwtUtils.getRealName(claims))
                .roles(jwtUtils.getRoles(claims))
                .tenantType(jwtUtils.getTenantType(claims))
                .tenantId(jwtUtils.getTenantId(claims))
                .orgId(jwtUtils.getOrgId(claims))
                .build();
        String clientType = jwtUtils.getClientType(claims);
        // 续签时保留原 userType
        UserType userType = parseUserType(oldToken);
        return generateToken(ctx, clientType == null ? CommonConstant.CLIENT_TYPE_PC : clientType,
                userType == null ? UserType.INTERNAL : userType);
    }

    /**
     * 根据 userType + clientType 解析 Token 有效期（秒）
     *
     * <p>优先级：userType > clientType > 默认（INTERNAL 8h）</p>
     */
    private long resolveTtl(UserType userType, String clientType) {
        // 优先按 userType 决定有效期
        if (userType != null) {
            return switch (userType) {
                case INTERNAL -> internalTtl;
                case AGENT -> agentTtl;
                case CUSTOMER -> customerTtl;
            };
        }
        // userType 缺失时按 clientType 回退（兼容旧 Token 续签）
        if (clientType == null) {
            return CommonConstant.TOKEN_TTL_PC;
        }
        return switch (clientType) {
            case CommonConstant.CLIENT_TYPE_MOBILE -> CommonConstant.TOKEN_TTL_MOBILE;
            case CommonConstant.CLIENT_TYPE_CUSTOMER -> CommonConstant.TOKEN_TTL_CUSTOMER;
            default -> CommonConstant.TOKEN_TTL_PC;
        };
    }

    /**
     * 从 UserContext.tenantType 推导 UserType（向后兼容）
     */
    private UserType resolveUserTypeFromContext(UserContext ctx) {
        if (ctx == null || ctx.getTenantType() == null) {
            return UserType.INTERNAL;
        }
        return switch (ctx.getTenantType()) {
            case CommonConstant.TENANT_TYPE_AGENT -> UserType.AGENT;
            case CommonConstant.TENANT_TYPE_CUSTOMER -> UserType.CUSTOMER;
            default -> UserType.INTERNAL;
        };
    }

    /**
     * 从 Authorization 头中提取 Token
     */
    public static String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (authHeader.startsWith(CommonConstant.BEARER_PREFIX)) {
            return authHeader.substring(CommonConstant.BEARER_PREFIX.length()).trim();
        }
        return authHeader.trim();
    }

    /**
     * 获取 Token 持有者角色列表（便捷方法）
     */
    public List<String> getRoles(String token) {
        Claims claims = jwtUtils.parseToken(token);
        return jwtUtils.getRoles(claims);
    }
}
