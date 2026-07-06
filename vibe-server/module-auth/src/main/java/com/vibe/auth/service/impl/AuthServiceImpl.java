package com.vibe.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.entity.AgentEngineerEntity;
import com.vibe.agent.mapper.AgentEngineerMapper;
import com.vibe.auth.domain.AuthUser;
import com.vibe.auth.domain.enums.UserType;
import com.vibe.auth.dto.AgentLoginDTO;
import com.vibe.auth.dto.CustomerLoginDTO;
import com.vibe.auth.dto.LoginDTO;
import com.vibe.auth.provider.JwtTokenProvider;
import com.vibe.auth.service.AuthService;
import com.vibe.auth.vo.LoginVO;
import com.vibe.common.constant.CommonConstant;
import com.vibe.common.constant.RedisKeyConstant;
import com.vibe.common.context.UserContext;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.project.entity.CustomerEntity;
import com.vibe.project.mapper.CustomerMapper;
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
 * <p>多类型用户认证（Task 5）：</p>
 * <ul>
 *   <li>{@link #login} —— 内部用户账号密码登录（userType=INTERNAL，8h）</li>
 *   <li>{@link #agentLogin} —— 代理商工程师手机号验证码登录（userType=AGENT，7d）</li>
 *   <li>{@link #customerLogin} —— 客户手机号验证码登录（userType=CUSTOMER，2h）</li>
 * </ul>
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

    // Task 5: 独立 Mapper（从 module-agent / module-project 解耦查询代理商/客户）
    private final AgentEngineerMapper agentEngineerMapper;
    private final CustomerMapper customerMapper;

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

        // 6. 签发 Token（userType 由 tenantType 推导，内部用户=INTERNAL）
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

    @Override
    public LoginVO agentLogin(AgentLoginDTO dto) {
        // 1. 校验短信验证码
        verifySmsCode(dto.getPhone(), dto.getSmsCode());

        // 2. 按手机号查询代理商工程师
        AgentEngineerEntity agent = agentEngineerMapper.selectOne(
                new LambdaQueryWrapper<AgentEngineerEntity>()
                        .eq(AgentEngineerEntity::getPhone, dto.getPhone()));
        if (agent == null) {
            log.warn("[Auth] 代理商工程师不存在 phone={}", dto.getPhone());
            throw BusinessException.of(ResultCode.ACCOUNT_NOT_FOUND);
        }

        // 3. 状态校验
        if (!AgentConstant.ENGINEER_STATUS_ACTIVE.equalsIgnoreCase(agent.getStatus())) {
            throw BusinessException.of(ResultCode.ACCOUNT_DISABLED);
        }

        // 4. 构建认证用户聚合根
        AuthUser authUser = AuthUser.ofAgent(agent, List.of(AgentConstant.ROLE_AGENT_ENGINEER),
                Collections.emptyList());

        // 5. 组装 UserContext
        UserContext ctx = UserContext.builder()
                .userId(authUser.getId())
                .userName(authUser.getUsername())
                .realName(authUser.getRealName())
                .roles(authUser.getRoles())
                .tenantType(CommonConstant.TENANT_TYPE_AGENT)
                .tenantId(authUser.getTenantId())
                .build();

        // 6. 签发 AGENT 类型 Token（7d）
        String token = jwtTokenProvider.generateToken(ctx, CommonConstant.CLIENT_TYPE_AGENT,
                UserType.AGENT);

        return buildLoginVOFromAuthUser(token, authUser, CommonConstant.CLIENT_TYPE_AGENT);
    }

    @Override
    public LoginVO customerLogin(CustomerLoginDTO dto) {
        // 1. 校验短信验证码
        verifySmsCode(dto.getPhone(), dto.getSmsCode());

        // 2. 按联系人电话查询客户
        CustomerEntity customer = customerMapper.selectOne(
                new LambdaQueryWrapper<CustomerEntity>()
                        .eq(CustomerEntity::getContactPhone, dto.getPhone()));

        // 3. 构建 UserContext
        UserContext ctx;
        AuthUser authUser;
        if (customer == null) {
            // 客户联系人未在 customer 表登记，仍允许登录（仅以手机号作为身份）
            log.info("[Auth] 客户联系人未登记，按手机号签发临时 Token phone={}", dto.getPhone());
            ctx = UserContext.builder()
                    .userName(dto.getPhone())
                    .tenantType(CommonConstant.TENANT_TYPE_CUSTOMER)
                    .roles(List.of("CUSTOMER"))
                    .build();
            String token = jwtTokenProvider.generateToken(ctx, CommonConstant.CLIENT_TYPE_CUSTOMER,
                    UserType.CUSTOMER);
            return buildLoginVOFromContext(token, CommonConstant.CLIENT_TYPE_CUSTOMER, ctx);
        }

        // 4. 构建 CUSTOMER 类型聚合根
        authUser = AuthUser.ofCustomer(customer, List.of("CUSTOMER"), Collections.emptyList());
        ctx = UserContext.builder()
                .userId(authUser.getId())
                .userName(authUser.getUsername())
                .realName(authUser.getRealName())
                .roles(authUser.getRoles())
                .tenantType(CommonConstant.TENANT_TYPE_CUSTOMER)
                .tenantId(authUser.getTenantId())
                .build();

        // 5. 签发 CUSTOMER 类型 Token（2h）
        String token = jwtTokenProvider.generateToken(ctx, CommonConstant.CLIENT_TYPE_CUSTOMER,
                UserType.CUSTOMER);

        return buildLoginVOFromAuthUser(token, authUser, CommonConstant.CLIENT_TYPE_CUSTOMER);
    }

    /**
     * 校验短信验证码（验证码使用后立即删除）
     *
     * @param phone   手机号
     * @param smsCode 验证码
     */
    private void verifySmsCode(String phone, String smsCode) {
        String cached = redisUtils.getStr(RedisKeyConstant.smsCode(phone));
        if (cached == null || !cached.equals(smsCode)) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        // 验证码使用后立即删除
        redisUtils.delete(RedisKeyConstant.smsCode(phone));
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
        // 续签时保留原 userType
        UserType userType = jwtTokenProvider.parseUserType(token);
        String newToken = jwtTokenProvider.generateToken(ctx, clientType, userType);
        // 刷新时无法重新查库，直接用 UserContext 信息组装
        return buildLoginVOFromContext(newToken, clientType, ctx);
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
     * 构建登录响应（基于 AuthUser 聚合根）。
     * 用于 agentLogin / customerLogin 流程。
     */
    private LoginVO buildLoginVOFromAuthUser(String token, AuthUser authUser, String clientType) {
        long ttl = resolveTtl(clientType);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(ttl);
        vo.setRefreshToken("");

        LoginVO.UserInfo userInfo = new LoginVO.UserInfo();
        userInfo.setUserId(authUser.getId());
        userInfo.setUserName(authUser.getUsername());
        userInfo.setRealName(authUser.getRealName());
        userInfo.setRoles(authUser.getRoles());
        userInfo.setTenantType(authUser.getUserType() == UserType.AGENT
                ? CommonConstant.TENANT_TYPE_AGENT : CommonConstant.TENANT_TYPE_CUSTOMER);
        userInfo.setTenantId(authUser.getTenantId());
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
            case CommonConstant.CLIENT_TYPE_AGENT -> CommonConstant.TOKEN_TTL_MOBILE;
            default -> CommonConstant.TOKEN_TTL_PC;
        };
    }
}
