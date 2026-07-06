package com.vibe.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户登录请求 DTO（手机号 + 短信验证码）
 *
 * <p>客户通过 H5 端手机号 + 短信验证码登录，
 * 签发 CUSTOMER 类型 Token（默认 2h 有效期）。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户登录请求（手机号 + 短信验证码）")
public class CustomerLoginDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号（客户联系人电话）", example = "13900139000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "短信验证码", example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;
}
