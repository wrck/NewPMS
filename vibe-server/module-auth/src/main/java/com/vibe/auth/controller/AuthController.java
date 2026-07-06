package com.vibe.auth.controller;

import com.vibe.auth.dto.AgentLoginDTO;
import com.vibe.auth.dto.ChangePasswordDTO;
import com.vibe.auth.dto.CustomerLoginDTO;
import com.vibe.auth.dto.LoginDTO;
import com.vibe.auth.service.AuthService;
import com.vibe.auth.vo.LoginVO;
import com.vibe.common.constant.CommonConstant;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.Result;
import com.vibe.common.result.ResultCode;
import com.vibe.system.service.SysUserService;
import com.vibe.system.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 *
 * <p>提供多类型用户认证体系接口：</p>
 * <ul>
 *   <li>{@code POST /auth/login} —— 内部用户账号密码登录（PC/MOBILE 端，Token 8h）</li>
 *   <li>{@code POST /auth/agent/login} —— 代理商工程师手机号验证码登录（H5 端，Token 7d）</li>
 *   <li>{@code POST /auth/customer/login} —— 客户手机号验证码登录（H5 端，Token 2h）</li>
 *   <li>{@code POST /auth/logout} —— 登出</li>
 *   <li>{@code POST /auth/refresh} —— 刷新 Token</li>
 *   <li>{@code GET  /auth/me} —— 当前登录用户信息</li>
 *   <li>{@code POST /auth/change-password} —— 当前用户修改密码</li>
 *   <li>{@code POST /auth/sms/send} —— 发送短信验证码</li>
 * </ul>
 *
 * <p>三类用户为独立认证体系：内部用户、代理商、客户分别走不同登录端点，
 * 签发不同 userType 的 Token（INTERNAL/AGENT/CUSTOMER），有效期独立配置。</p>
 *
 * @author vibe
 */
@Tag(name = "认证授权", description = "登录/登出/Token 刷新/短信验证码登录/多类型用户认证/当前用户信息/修改密码")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserService sysUserService;

    @Operation(summary = "内部用户账号密码登录", description = "PC/MOBILE 端账号密码登录，返回 JWT Token（userType=INTERNAL，8h）")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "代理商工程师登录", description = "代理商 H5 端手机号 + 短信验证码登录，签发 AGENT 类型 Token（7d）")
    @PostMapping("/agent/login")
    public Result<LoginVO> agentLogin(@RequestBody @Valid AgentLoginDTO dto) {
        return Result.success(authService.agentLogin(dto));
    }

    @Operation(summary = "客户手机号验证码登录", description = "客户 H5 入口，发送验证码后用手机号 + 验证码登录，签发 CUSTOMER 类型 Token（2h）")
    @PostMapping("/customer/login")
    public Result<LoginVO> customerLogin(@RequestBody @Valid CustomerLoginDTO dto) {
        return Result.success(authService.customerLogin(dto));
    }

    @Operation(summary = "登出", description = "将当前 Token 加入黑名单，强制下线")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(CommonConstant.HEADER_AUTHORIZATION);
        String token = authHeader == null ? null : authHeader.substring(CommonConstant.BEARER_PREFIX.length());
        authService.logout(token);
        return Result.success();
    }

    @Operation(summary = "刷新 Token", description = "用旧 Token 换取新 Token（剩余 < 2h 时调用）")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(HttpServletRequest request) {
        String authHeader = request.getHeader(CommonConstant.HEADER_AUTHORIZATION);
        String token = authHeader == null ? null : authHeader.substring(CommonConstant.BEARER_PREFIX.length());
        return Result.success(authService.refresh(token));
    }

    @Operation(summary = "发送短信验证码", description = "代理商/客户登录前发送短信验证码（MVP 阶段用日志/控制台模拟）")
    @PostMapping("/sms/send")
    public Result<Void> sendSmsCode(@RequestParam String phone) {
        // TODO Task 8.3: 调用短信适配器，MVP 阶段日志模拟
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息", description = "前端 getUserInfo 调用，返回用户基本信息、角色编码、权限标识")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }
        return Result.success(sysUserService.getCurrentUserInfo());
    }

    @Operation(summary = "修改密码", description = "当前登录用户修改密码，需提供旧密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }
        authService.changePassword(ctx.getUserId(), dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }
}
