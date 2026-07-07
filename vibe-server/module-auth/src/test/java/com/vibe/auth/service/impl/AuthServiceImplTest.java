package com.vibe.auth.service.impl;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.entity.AgentEngineerEntity;
import com.vibe.agent.mapper.AgentEngineerMapper;
import com.vibe.auth.domain.enums.UserType;
import com.vibe.auth.dto.AgentLoginDTO;
import com.vibe.auth.dto.CustomerLoginDTO;
import com.vibe.auth.dto.LoginDTO;
import com.vibe.auth.provider.JwtTokenProvider;
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
import com.vibe.system.vo.SysUserVO;
import com.vibe.utils.JwtUtils;
import com.vibe.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务实现单元测试（Task 3 SubTask 3.1）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>login：账号不存在 / 账号禁用 / 密码错误 / 用户名命中 / 手机号回退 / Token 签发 / 更新登录时间失败兜底</li>
 *   <li>agentLogin：验证码错误 / 工程师不存在 / 工程师禁用 / 正常签发 AGENT Token</li>
 *   <li>customerLogin：验证码错误 / 客户不存在仍允许 / 客户存在签发 CUSTOMER Token</li>
 *   <li>logout：空 Token / 正常加入黑名单 / Token 解析失败兜底</li>
 *   <li>refresh：基于旧 Token 重新签发</li>
 *   <li>changePassword：委托给 SysUserService</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("认证服务 AuthServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private RedisUtils redisUtils;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AgentEngineerMapper agentEngineerMapper;
    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void tearDown() {
        // 清理 ThreadLocal，避免跨用例污染
        com.vibe.common.context.UserContextHolder.clear();
    }

    /* ============ login 内部用户登录 ============ */

    @Nested
    @DisplayName("login 内部用户登录")
    class LoginTest {

        @Test
        @DisplayName("账号不存在抛 ACCOUNT_NOT_FOUND")
        void should_throw_account_not_found_when_user_missing() {
            when(sysUserService.findByUsername("nobody")).thenReturn(null);
            LoginDTO dto = buildLoginDto("nobody", "pwd");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(dto),
                    "账号不存在应抛 BusinessException");
            assertEquals(ResultCode.ACCOUNT_NOT_FOUND.getCode(), ex.getCode());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("用户名为空时按手机号回退查询，仍命中失败抛 ACCOUNT_NOT_FOUND")
        void should_fallback_to_phone_when_username_lookup_misses() {
            when(sysUserService.findByUsername(any())).thenReturn(null);
            when(sysUserService.findByPhone("13900000000")).thenReturn(null);
            LoginDTO dto = buildLoginDto("13900000000", "pwd");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(dto));
            assertEquals(ResultCode.ACCOUNT_NOT_FOUND.getCode(), ex.getCode());
            verify(sysUserService).findByPhone("13900000000");
        }

        @Test
        @DisplayName("账号被禁用抛 ACCOUNT_DISABLED")
        void should_throw_account_disabled_when_status_not_active() {
            SysUserVO user = buildUser(1L, "admin", "DISABLED", "hash");
            when(sysUserService.findByUsername("admin")).thenReturn(user);
            LoginDTO dto = buildLoginDto("admin", "pwd");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(dto));
            assertEquals(ResultCode.ACCOUNT_DISABLED.getCode(), ex.getCode());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("密码校验失败抛 PASSWORD_ERROR")
        void should_throw_password_error_when_matches_false() {
            SysUserVO user = buildUser(1L, "admin", SystemConstant.USER_STATUS_ACTIVE, "hash");
            when(sysUserService.findByUsername("admin")).thenReturn(user);
            when(passwordEncoder.matches("raw", "hash")).thenReturn(false);
            LoginDTO dto = buildLoginDto("admin", "raw");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(dto));
            assertEquals(ResultCode.PASSWORD_ERROR.getCode(), ex.getCode());
            verify(jwtTokenProvider, never()).generateToken(any(), any());
        }

        @Test
        @DisplayName("正常登录：签发 Token、返回 userInfo、更新最后登录时间")
        void should_login_success_and_return_login_vo() {
            SysUserVO user = buildUser(7L, "admin", SystemConstant.USER_STATUS_ACTIVE, "hash");
            user.setRealName("管理员");
            user.setTenantType(CommonConstant.TENANT_TYPE_INTERNAL);
            when(sysUserService.findByUsername("admin")).thenReturn(user);
            when(passwordEncoder.matches("raw", "hash")).thenReturn(true);
            when(jwtTokenProvider.generateToken(any(UserContext.class), eq(CommonConstant.CLIENT_TYPE_PC)))
                    .thenReturn("token-xyz");

            LoginVO vo = authService.login(buildLoginDto("admin", "raw"));

            assertNotNull(vo);
            assertEquals("token-xyz", vo.getToken());
            assertNotNull(vo.getUserInfo());
            assertEquals(7L, vo.getUserInfo().getUserId());
            assertEquals("admin", vo.getUserInfo().getUserName());
            assertEquals("管理员", vo.getUserInfo().getRealName());
            assertEquals(CommonConstant.TENANT_TYPE_INTERNAL, vo.getUserInfo().getTenantType());
            verify(sysUserService).updateLastLoginTime(7L);
        }

        @Test
        @DisplayName("更新最后登录时间失败不影响登录主流程")
        void should_not_fail_when_update_last_login_time_throws() {
            SysUserVO user = buildUser(7L, "admin", SystemConstant.USER_STATUS_ACTIVE, "hash");
            when(sysUserService.findByUsername("admin")).thenReturn(user);
            when(passwordEncoder.matches("raw", "hash")).thenReturn(true);
            when(jwtTokenProvider.generateToken(any(UserContext.class), any())).thenReturn("t");
            doThrow(new RuntimeException("DB 不可用")).when(sysUserService).updateLastLoginTime(7L);

            LoginVO vo = authService.login(buildLoginDto("admin", "raw"));

            assertNotNull(vo);
            assertEquals("t", vo.getToken());
        }

        @Test
        @DisplayName("clientId 为空时默认 PC")
        void should_default_to_pc_when_client_id_missing() {
            SysUserVO user = buildUser(1L, "u", SystemConstant.USER_STATUS_ACTIVE, "h");
            when(sysUserService.findByUsername("u")).thenReturn(user);
            when(passwordEncoder.matches(any(), any())).thenReturn(true);
            when(jwtTokenProvider.generateToken(any(UserContext.class), eq(CommonConstant.CLIENT_TYPE_PC)))
                    .thenReturn("pc-token");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("u");
            dto.setPassword("p");
            LoginVO vo = authService.login(dto);

            assertEquals("pc-token", vo.getToken());
            // PC TTL = 8h
            assertEquals(CommonConstant.TOKEN_TTL_PC, vo.getExpiresIn());
        }
    }

    /* ============ agentLogin 代理商登录 ============ */

    @Nested
    @DisplayName("agentLogin 代理商工程师登录")
    class AgentLoginTest {

        @Test
        @DisplayName("短信验证码错误抛 SMS_CODE_ERROR")
        void should_throw_sms_code_error_when_code_mismatch() {
            when(redisUtils.getStr(RedisKeyConstant.smsCode("13800138000"))).thenReturn("999999");
            AgentLoginDTO dto = buildAgentDto("13800138000", "123456");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.agentLogin(dto));
            assertEquals(ResultCode.SMS_CODE_ERROR.getCode(), ex.getCode());
            verify(agentEngineerMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("验证码为空时抛 SMS_CODE_ERROR")
        void should_throw_sms_code_error_when_cache_missing() {
            when(redisUtils.getStr(RedisKeyConstant.smsCode("13800138000"))).thenReturn(null);
            AgentLoginDTO dto = buildAgentDto("13800138000", "123456");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.agentLogin(dto));
            assertEquals(ResultCode.SMS_CODE_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("代理商工程师不存在抛 ACCOUNT_NOT_FOUND")
        void should_throw_account_not_found_when_agent_missing() {
            when(redisUtils.getStr(RedisKeyConstant.smsCode("13800138000"))).thenReturn("123456");
            when(agentEngineerMapper.selectOne(any())).thenReturn(null);
            AgentLoginDTO dto = buildAgentDto("13800138000", "123456");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.agentLogin(dto));
            assertEquals(ResultCode.ACCOUNT_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("代理商工程师被禁用抛 ACCOUNT_DISABLED")
        void should_throw_account_disabled_when_agent_disabled() {
            when(redisUtils.getStr(RedisKeyConstant.smsCode("13800138000"))).thenReturn("123456");
            AgentEngineerEntity agent = buildAgent(20L, "13800138000", "DISABLED");
            when(agentEngineerMapper.selectOne(any())).thenReturn(agent);
            AgentLoginDTO dto = buildAgentDto("13800138000", "123456");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.agentLogin(dto));
            assertEquals(ResultCode.ACCOUNT_DISABLED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常登录：验证码使用后删除、签发 AGENT Token、tenantType=AGENT")
        void should_login_success_and_clear_sms_code() {
            when(redisUtils.getStr(RedisKeyConstant.smsCode("13800138000"))).thenReturn("123456");
            AgentEngineerEntity agent = buildAgent(20L, "13800138000", AgentConstant.ENGINEER_STATUS_ACTIVE);
            when(agentEngineerMapper.selectOne(any())).thenReturn(agent);
            when(jwtTokenProvider.generateToken(any(UserContext.class),
                    eq(CommonConstant.CLIENT_TYPE_AGENT), eq(UserType.AGENT)))
                    .thenReturn("agent-token");

            LoginVO vo = authService.agentLogin(buildAgentDto("13800138000", "123456"));

            assertNotNull(vo);
            assertEquals("agent-token", vo.getToken());
            assertEquals(CommonConstant.TENANT_TYPE_AGENT, vo.getUserInfo().getTenantType());
            assertEquals(20L, vo.getUserInfo().getUserId());
            // 验证码使用后应被删除
            verify(redisUtils).delete(RedisKeyConstant.smsCode("13800138000"));
        }
    }

    /* ============ customerLogin 客户登录 ============ */

    @Nested
    @DisplayName("customerLogin 客户登录")
    class CustomerLoginTest {

        @Test
        @DisplayName("短信验证码错误抛 SMS_CODE_ERROR")
        void should_throw_sms_code_error_when_customer_code_mismatch() {
            when(redisUtils.getStr(RedisKeyConstant.smsCode("13900000000"))).thenReturn("000000");
            CustomerLoginDTO dto = buildCustomerDto("13900000000", "123456");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.customerLogin(dto));
            assertEquals(ResultCode.SMS_CODE_ERROR.getCode(), ex.getCode());
            verify(customerMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("客户联系人未登记仍允许登录（按手机号签发临时 Token）")
        void should_allow_login_when_customer_not_registered() {
            when(redisUtils.getStr(RedisKeyConstant.smsCode("13900000000"))).thenReturn("123456");
            when(customerMapper.selectOne(any())).thenReturn(null);
            when(jwtTokenProvider.generateToken(any(UserContext.class),
                    eq(CommonConstant.CLIENT_TYPE_CUSTOMER), eq(UserType.CUSTOMER)))
                    .thenReturn("customer-tmp-token");

            LoginVO vo = authService.customerLogin(buildCustomerDto("13900000000", "123456"));

            assertNotNull(vo);
            assertEquals("customer-tmp-token", vo.getToken());
            assertEquals(CommonConstant.TENANT_TYPE_CUSTOMER, vo.getUserInfo().getTenantType());
            assertEquals("13900000000", vo.getUserInfo().getUserName());
        }

        @Test
        @DisplayName("客户已登记：签发 CUSTOMER Token、tenantId 为客户ID")
        void should_login_success_when_customer_registered() {
            when(redisUtils.getStr(RedisKeyConstant.smsCode("13900000000"))).thenReturn("123456");
            CustomerEntity customer = buildCustomer(99L, "13900000000", "张三");
            when(customerMapper.selectOne(any())).thenReturn(customer);
            when(jwtTokenProvider.generateToken(any(UserContext.class),
                    eq(CommonConstant.CLIENT_TYPE_CUSTOMER), eq(UserType.CUSTOMER)))
                    .thenReturn("customer-token");

            LoginVO vo = authService.customerLogin(buildCustomerDto("13900000000", "123456"));

            assertEquals("customer-token", vo.getToken());
            assertEquals(99L, vo.getUserInfo().getUserId());
            assertEquals(99L, vo.getUserInfo().getTenantId());
            assertEquals(CommonConstant.TENANT_TYPE_CUSTOMER, vo.getUserInfo().getTenantType());
            assertEquals("张三", vo.getUserInfo().getRealName());
        }
    }

    /* ============ logout 登出 ============ */

    @Nested
    @DisplayName("logout 登出")
    class LogoutTest {

        @Test
        @DisplayName("Token 为空直接返回，不操作 Redis")
        void should_do_nothing_when_token_blank() {
            authService.logout(null);
            authService.logout("  ");
            verify(redisUtils, never()).set(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("正常登出：将 tokenId 加入黑名单，TTL 为 Token 剩余有效期")
        void should_add_token_to_blacklist() {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(claims.getId()).thenReturn("tokenId-123");
            when(jwtUtils.parseToken("raw-token")).thenReturn(claims);
            when(jwtUtils.getRemainingTtl("raw-token")).thenReturn(3600L);

            authService.logout("raw-token");

            verify(redisUtils).set(eq(RedisKeyConstant.tokenBlacklist("tokenId-123")),
                    eq("1"), eq(Duration.ofSeconds(3600L)));
        }

        @Test
        @DisplayName("Token 解析失败时不抛错，仅记日志")
        void should_swallow_parse_exception() {
            when(jwtUtils.parseToken("bad-token"))
                    .thenThrow(new RuntimeException("jwt 解析失败"));

            // 不应抛错
            authService.logout("bad-token");

            verify(redisUtils, never()).set(any(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("tokenId 为空时不加入黑名单")
        void should_skip_blacklist_when_token_id_null() {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(claims.getId()).thenReturn(null);
            when(jwtUtils.parseToken("raw")).thenReturn(claims);
            when(jwtUtils.getRemainingTtl("raw")).thenReturn(100L);

            authService.logout("raw");

            verify(redisUtils, never()).set(any(), any(), any(Duration.class));
        }
    }

    /* ============ refresh Token 刷新 ============ */

    @Nested
    @DisplayName("refresh Token 刷新")
    class RefreshTest {

        @Test
        @DisplayName("刷新 Token：保留原 userType 与 clientType")
        void should_refresh_token_keep_user_type_and_client_type() {
            UserContext ctx = UserContext.builder()
                    .userId(1L)
                    .userName("u")
                    .clientType(CommonConstant.CLIENT_TYPE_PC)
                    .tenantType(CommonConstant.TENANT_TYPE_INTERNAL)
                    .roles(List.of("PM"))
                    .build();
            when(jwtTokenProvider.parseUserContext("old")).thenReturn(ctx);
            when(jwtTokenProvider.parseUserType("old")).thenReturn(UserType.INTERNAL);
            when(jwtTokenProvider.generateToken(ctx, CommonConstant.CLIENT_TYPE_PC, UserType.INTERNAL))
                    .thenReturn("new-token");

            LoginVO vo = authService.refresh("old");

            assertNotNull(vo);
            assertEquals("new-token", vo.getToken());
            assertEquals(1L, vo.getUserInfo().getUserId());
            assertEquals(CommonConstant.TENANT_TYPE_INTERNAL, vo.getUserInfo().getTenantType());
        }

        @Test
        @DisplayName("刷新 Token：clientType 缺失时默认 PC")
        void should_default_to_pc_when_client_type_missing() {
            UserContext ctx = UserContext.builder()
                    .userId(1L).userName("u").build();
            when(jwtTokenProvider.parseUserContext("old")).thenReturn(ctx);
            when(jwtTokenProvider.parseUserType("old")).thenReturn(UserType.INTERNAL);
            when(jwtTokenProvider.generateToken(ctx, CommonConstant.CLIENT_TYPE_PC, UserType.INTERNAL))
                    .thenReturn("new-token");

            LoginVO vo = authService.refresh("old");

            assertEquals("new-token", vo.getToken());
        }
    }

    /* ============ changePassword 修改密码 ============ */

    @Nested
    @DisplayName("changePassword 修改密码")
    class ChangePasswordTest {

        @Test
        @DisplayName("正常修改密码委托给 SysUserService")
        void should_delegate_to_sys_user_service() {
            authService.changePassword(1L, "old", "new");
            verify(sysUserService).changePassword(1L, "old", "new");
        }

        @Test
        @DisplayName("委托异常向上抛出")
        void should_propagate_exception_from_user_service() {
            doThrow(new BusinessException(ResultCode.PASSWORD_ERROR))
                    .when(sysUserService).changePassword(1L, "wrong", "new");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.changePassword(1L, "wrong", "new"));
            assertEquals(ResultCode.PASSWORD_ERROR.getCode(), ex.getCode());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private LoginDTO buildLoginDto(String username, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    private SysUserVO buildUser(Long id, String username, String status, String pwd) {
        SysUserVO vo = new SysUserVO();
        vo.setId(id);
        vo.setUsername(username);
        vo.setStatus(status);
        vo.setPassword(pwd);
        vo.setRoles(Collections.emptyList());
        return vo;
    }

    private AgentLoginDTO buildAgentDto(String phone, String smsCode) {
        AgentLoginDTO dto = new AgentLoginDTO();
        dto.setPhone(phone);
        dto.setSmsCode(smsCode);
        return dto;
    }

    private AgentEngineerEntity buildAgent(Long id, String phone, String status) {
        AgentEngineerEntity agent = new AgentEngineerEntity();
        agent.setId(id);
        agent.setPhone(phone);
        agent.setName("代理商工程师" + id);
        agent.setStatus(status);
        agent.setAgentCompanyId(100L);
        return agent;
    }

    private CustomerLoginDTO buildCustomerDto(String phone, String smsCode) {
        CustomerLoginDTO dto = new CustomerLoginDTO();
        dto.setPhone(phone);
        dto.setSmsCode(smsCode);
        return dto;
    }

    private CustomerEntity buildCustomer(Long id, String phone, String contactName) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(id);
        customer.setContactPhone(phone);
        customer.setContactName(contactName);
        return customer;
    }
}
