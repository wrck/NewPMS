package com.vibe.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前登录用户信息（含角色与权限）
 *
 * <p>字段命名与前端 vibe-web 的 {@code UserInfo} 类型对齐：
 * <ul>
 *   <li>{@code userId} —— 用户ID（前端字段名）</li>
 *   <li>{@code userName} —— 用户名（前端字段名，驼峰大写 N）</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String userName;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "状态 ACTIVE/DISABLED")
    private String status;

    @Schema(description = "租户类型")
    private String tenantType;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "权限标识列表")
    private List<String> permissions;
}
