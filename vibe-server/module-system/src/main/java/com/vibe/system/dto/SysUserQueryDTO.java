package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 用户分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户分页查询")
public class SysUserQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名/真实姓名/手机号（模糊）")
    private String keyword;

    @Schema(description = "状态 ACTIVE/DISABLED")
    private String status;

    @Schema(description = "租户类型")
    private String tenantType;

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "角色ID")
    private Long roleId;
}
