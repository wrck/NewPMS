package com.vibe.project.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 项目分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目分页查询")
public class ProjectQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目名称/编号（模糊）")
    private String keyword;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "项目经理ID")
    private Long pmId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "执行模式")
    private String executeMode;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "项目类型")
    private String projectType;
}
