package com.vibe.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户新增/编辑 DTO
 *
 * <p>JSON 字段命名与前端 vibe-web 的 {@code SysUserDTO} 类型对齐：
 * <ul>
 *   <li>{@code userName} —— 登录账号（Java 字段为 {@code username}，JSON 名对齐前端）</li>
 *   <li>{@code status} —— 1-启用 0-禁用（数字类型，Service 层转为 ACTIVE/DISABLED）</li>
 *   <li>{@code roleCodes} —— 角色编码列表（字符串数组，Service 层转为 roleId）</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "用户新增/编辑")
public class SysUserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID（编辑时必填）")
    private Long id;

    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 64, message = "登录账号长度不能超过64")
    @JsonProperty("userName")
    private String username;

    @Schema(description = "密码（新增时必填，明文，BCrypt 加密存储）")
    @Size(min = 6, max = 64, message = "密码长度需在6-64之间")
    private String password;

    @Schema(description = "真实姓名")
    @Size(max = 64, message = "真实姓名长度不能超过64")
    private String realName;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过20")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "租户类型 INTERNAL/AGENT/CUSTOMER")
    private String tenantType;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "角色编码列表")
    private List<String> roleCodes;
}
