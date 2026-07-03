package com.vibe.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 账号密码登录请求 DTO
 *
 * <p>字段命名与前端 vibe-web 对齐：
 * <ul>
 *   <li>{@code username} —— 用户名/手机号（前端字段名）</li>
 *   <li>{@code clientId} —— 客户端类型 PC/MOBILE/AGENT/CUSTOMER（前端字段名）</li>
 *   <li>{@code captchaKey} —— 验证码会话 ID（前端字段名）</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "账号密码登录请求")
public class LoginDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名/手机号", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账号不能为空")
    private String username;

    @Schema(description = "密码（明文，由前端加密传输亦可）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "图形验证码（启用验证码时必填）")
    private String captcha;

    @Schema(description = "验证码会话ID")
    private String captchaKey;

    @Schema(description = "客户端类型：PC / MOBILE / AGENT / CUSTOMER", example = "PC")
    private String clientId;
}
