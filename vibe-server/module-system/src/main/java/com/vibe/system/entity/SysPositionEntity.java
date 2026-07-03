package com.vibe.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 岗位实体（sys_position）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_position")
@Schema(description = "岗位")
public class SysPositionEntity extends SysBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "岗位名称")
    private String positionName;

    @Schema(description = "岗位编码")
    private String positionCode;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
