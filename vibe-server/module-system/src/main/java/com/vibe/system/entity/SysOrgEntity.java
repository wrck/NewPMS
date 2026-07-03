package com.vibe.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 组织架构实体（sys_org，树形）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
@Schema(description = "组织架构")
public class SysOrgEntity extends SysBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "父组织ID（0为根）")
    private Long parentId;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "组织编码")
    private String orgCode;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
