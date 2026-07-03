package com.vibe.auth.service.impl;

import com.vibe.auth.dto.LoginDTO;
import com.vibe.auth.provider.JwtTokenProvider;
import com.vibe.auth.service.AuthService;
import com.vibe.auth.vo.LoginVO;
import com.vibe.common.constant.CommonConstant;
import com.vibe.common.constant.RedisKeyConstant;
import com.vibe.common.context.UserContext;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.service.SysUserService;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysUserVO;
import com.vibe.utils.JwtUtils;
import com.vibe.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务实现
 *
 * <p>登录流程：</p>
 * <ol>
 *   <li>按用户名（或手机号）查询 sys_user，含角色列表</li>
 *   <li>校验账号状态（ACTIVE）</li>
 *   <li>BCrypt 密码校验</li>
 *   <li>组装 UserContext，签发 JWT Token</li>
 *   <li>组装嵌套 userInfo 的 LoginVO（与前端 LoginResult 对齐）</li>
 *   <li>更新最后登录时间</li>
 * </ol>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        String account = dto.getUsername();
        // 1. 按用户名查询，未命中再按手机号查询
        SysUserVO user = sysUserService.findByUsername(account);
        if (user == null && StringUtils.hasText(account)) {
            user = sysUserService.findByPhone(account);
        }
        if (user == null) {
            throw BusinessException.of(ResultCode.ACCOUNT_NOT_FOUND);
        }

        // 2. 账号状态校验
        if (!SystemConstant.USER_STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw BusinessException.of(ResultCode.ACCOUNT_DISABLED);
        }

        // 3. BCrypt 密码校验
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("[Auth] 密码校验失败 username={}", account);
            throw BusinessException.of(ResultCode.PASSWORD_ERROR);
        }

        // 4. 组装 UserContext
        List<String> roleCodes = extractRoleCodes(user.getRoles());
        UserContext ctx = UserContext.builder()
                .userId(user.getId())
                .userName(user.getUsername())
                .realName(user.getRealName())
                .roles(roleCodes)
                .tenantType(user.getTenantType() == null
                        ? CommonConstant.TENANT_TYPE_INTERNAL : user.getTenantType())
                .tenantId(user.getTenantId())
                .orgId(user.getOrgId())
                .build();

        // 5. 解析客户端类型（默认 PC），clientId/clientType 均兼容
        String clientType = resolveClientType(dto.getClientId());

        // 6. 签发 Token
        String token = jwtTokenProvider.generateToken(ctx, clientType);

        // 7. 更新最后登录时间（异步不影响登录主流程，失败忽略）
        try {
            sysUserService.updateLastLoginTime(user.getId());
        } catch (Exception e) {
            log.warn("[Auth] 更新最后登录时间失败 userId={}: {}", user.getId(), e.getMessage());
        }

        // 8. 组装嵌套 userInfo 的 LoginVO（与前端 LoginResult 对齐）
        return buildLoginVO(token, clientType, user, roleCodes);
    }

    /**
     * 从角色列表提取角色编码
     */
    private List<String> extractRoleCodes(List<RoleSimpleVO> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(RoleSimpleVO::getRoleCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    /**
     * 解析客户端类型，兼容 clientId（前端字段名）。
     * 默认返回 PC。
     */
    private String resolveClientType(String clientId) {
        return StringUtils.hasText(clientId) ? clientId : CommonConstant.CLIENT_TYPE_PC;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            String tokenId = claims.getId();
            long remaining = jwtUtils.getRemainingTtl(token);
            if (tokenId != null && remaining > 0) {
                // 加入黑名单，TTL = Token 剩余有效期
                redisUtils.set(RedisKeyConstant.tokenBlacklist(tokenId), "1",
                        Duration.ofSeconds(remaining));
            }
        } catch (Exception e) {
            log.warn("[Auth] 登出时 Token 解析失败，忽略: {}", e.getMessage());
        }
    }

    @Override
    public LoginVO refresh(String token) {
        // 解析旧 Token 生成新 Token（即使旧 Token 过期，也允许在宽限期内刷新）
        UserContext ctx = jwtTokenProvider.parseUserContext(token);
        String clientType = resolveClientType(ctx.getClientType());
        String newToken = jwtTokenProvider.generateToken(ctx, clientType);
        // 刷新时无法重新查库，直接用 UserContext 信息组装
        return buildLoginVOFromContext(newToken, clientType, ctx);
    }

    @Override
    public LoginVO customerLogin(String phone, String smsCode) {
        // 客户手机号 + 短信验证码登录
        String cached = redisUtils.getStr(RedisKeyConstant.smsCode(phone));
        if (cached == null || !cached.equals(smsCode)) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        // 验证码使用后立即删除
        redisUtils.delete(RedisKeyConstant.smsCode(phone));

        // TODO 查询客户联系人 -> 组装 UserContext
        UserContext ctx = UserContext.builder()
                .userName(phone)
                .tenantType(CommonConstant.TENANT_TYPE_CUSTOMER)
                .roles(List.of("CUSTOMER"))
                .build();
        String token = jwtTokenProvider.generateToken(ctx, CommonConstant.CLIENT_TYPE_CUSTOMER);
        return buildLoginVOFromContext(token, CommonConstant.CLIENT_TYPE_CUSTOMER, ctx);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        sysUserService.changePassword(userId, oldPassword, newPassword);
    }

    /**
     * 构建登录响应（基于完整的 SysUserVO，含角色列表）。
     * 用于 login 流程，可填充 avatar/email/phone/orgName 等完整字段。
     */
    private LoginVO buildLoginVO(String token, String clientType, SysUserVO user, List<String> roleCodes) {
        long ttl = resolveTtl(clientType);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(ttl);
        vo.setRefreshToken("");

        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setUserName(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setRoles(roleCodes);
        userInfo.setTenantType(user.getTenantType() == null
                ? CommonConstant.TENANT_TYPE_INTERNAL : user.getTenantType());
        userInfo.setTenantId(user.getTenantId());
        userInfo.setOrgId(user.getOrgId());
        userInfo.setOrgName(user.getOrgName());
        vo.setUserInfo(userInfo);
        return vo;
    }

    /**
     * 构建登录响应（仅基于 UserContext，字段不全）。
     * 用于 refresh / customerLogin 等不查库的场景。
     */
    private LoginVO buildLoginVOFromContext(String token, String clientType, UserContext ctx) {
        long ttl = resolveTtl(clientType);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(ttl);
        vo.setRefreshToken("");

        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setUserId(ctx.getUserId());
        userInfo.setUserName(ctx.getUserName());
        userInfo.setRealName(ctx.getRealName());
        userInfo.setRoles(ctx.getRoles());
        userInfo.setTenantType(ctx.getTenantType());
        userInfo.setTenantId(ctx.getTenantId());
        userInfo.setOrgId(ctx.getOrgId());
        vo.setUserInfo(userInfo);
        return vo;
    }

    /**
     * 根据客户端类型解析 Token 有效期（秒）
     */
    private long resolveTtl(String clientType) {
        if (clientType == null) {
            return CommonConstant.TOKEN_TTL_PC;
        }
        return switch (clientType) {
            case CommonConstant.CLIENT_TYPE_MOBILE -> CommonConstant.TOKEN_TTL_MOBILE;
            case CommonConstant.CLIENT_TYPE_CUSTOMER -> CommonConstant.TOKEN_TTL_CUSTOMER;
            default -> CommonConstant.TOKEN_TTL_PC;
        };
    }
}
