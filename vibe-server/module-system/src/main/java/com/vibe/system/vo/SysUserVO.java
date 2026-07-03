package com.vibe.system.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象（含角色列表）
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

    @Schema(description = "状态 ACTIVE/DISABLED")
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
