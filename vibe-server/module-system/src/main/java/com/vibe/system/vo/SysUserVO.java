package com.vibe.system.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vibe.common.serializer.UserStatusSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象（含角色列表）
 *
 * <p>JSON 字段命名与前端 vibe-web 的 {@code SysUser} 类型对齐：
 * <ul>
 *   <li>{@code userName} —— 登录账号（Java 字段为 {@code username}）</li>
 *   <li>{@code status} —— 序列化为数字 1/0（通过 {@link UserStatusSerializer}，
 *       Java 字段仍为 String ACTIVE/DISABLED）</li>
 *   <li>{@code roleNames} —— 角色名称列表（前端期望字段）</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "用户信息")
public class SysUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    @JsonProperty("userName")
    private String username;

    /**
     * 密码（BCrypt 加密）。
     * <p>仅供 module-auth 登录校验内部使用，{@link JsonIgnore} 确保不会序列化到 API 响应。</p>
     */
    @JsonIgnore
    @Schema(hidden = true)
    private String password;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像地址")
    private String avatar;

    /**
     * 状态 ACTIVE/DISABLED（数据库存储值）。
     * <p>序列化时通过 {@link UserStatusSerializer} 转为数字 1/0，与前端对齐。</p>
     */
    @Schema(description = "状态 1-启用 0-禁用")
    @JsonSerialize(using = UserStatusSerializer.class)
    private String status;

    @Schema(description = "租户类型 INTERNAL/AGENT/CUSTOMER")
    private String tenantType;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "所属组织名称")
    private String orgName;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "角色列表")
    private List<RoleSimpleVO> roles;
}
