package com.vibe.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织架构视图对象（树形）
 *
 * @author vibe
 */
@Data
@Schema(description = "组织架构")
public class SysOrgVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "组织ID")
    private Long id;

    @Schema(description = "父组织ID")
    private Long parentId;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "组织编码")
    private String orgCode;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子组织")
    private List<SysOrgVO> children;
}
