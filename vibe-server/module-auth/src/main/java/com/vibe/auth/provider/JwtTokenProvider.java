package com.vibe.auth.provider;

import com.vibe.common.constant.CommonConstant;
import com.vibe.common.context.UserContext;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtUtils jwtUtils;

    /**
     * 签发 Token
     *
     * @param ctx         用户上下文
     * @param clientType  客户端类型：PC / MOBILE / AGENT / CUSTOMER
     * @return JWT Token 字符串
     */
    public String generateToken(UserContext ctx, String clientType) {
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        long ttl = resolveTtl(clientType);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", ctx.getUserId());
        claims.put("userName", ctx.getUserName());
        claims.put("realName", ctx.getRealName());
        claims.put("roles", ctx.getRoles());
        claims.put("tenantType", ctx.getTenantType());
        claims.put("tenantId", ctx.getTenantId());
        claims.put("orgId", ctx.getOrgId());
        claims.put("clientType", clientType);
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
        return generateToken(ctx, clientType == null ? CommonConstant.CLIENT_TYPE_PC : clientType);
    }

    /**
     * 根据客户端类型解析 Token 有效期
     */
    private long resolveTtl(String clientType) {
        if (clientType == null) {
            return CommonConstant.TOKEN_TTL_PC;
        }
        return switch (clientType) {
            case CommonConstant.CLIENT_TYPE_MOBILE -> CommonConstant.TOKEN_TTL_MOBILE;
            case CommonConstant.CLIENT_TYPE_CUSTOMER -> CommonConstant.TOKEN_TTL_CUSTOMER;
            // 代理商与 PC 默认 8h
            case CommonConstant.CLIENT_TYPE_AGENT, CommonConstant.CLIENT_TYPE_PC -> CommonConstant.TOKEN_TTL_PC;
            default -> CommonConstant.TOKEN_TTL_PC;
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
