package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 用户分页查询 DTO
 *
 * <p>JSON 字段命名与前端 vibe-web 的 {@code SysUserQueryParams} 类型对齐：
 * <ul>
 *   <li>{@code userName} —— 用户名模糊查询</li>
 *   <li>{@code realName} —— 真实姓名模糊查询</li>
 *   <li>{@code phone} —— 手机号模糊查询</li>
 *   <li>{@code status} —— 1/0（数字）</li>
 *   <li>{@code roleCode} —— 角色编码（前端字段名）</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页查询")
public class SysUserQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名/真实姓名/手机号（模糊，兼容旧版）")
    private String keyword;

    @Schema(description = "用户名（模糊查询）")
    private String userName;

    @Schema(description = "真实姓名（模糊查询）")
    private String realName;

    @Schema(description = "手机号（模糊查询）")
    private String phone;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "租户类型")
    private String tenantType;

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "角色编码")
    private String roleCode;
}
