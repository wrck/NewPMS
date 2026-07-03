package com.vibe.project.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 项目模板分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目模板分页查询")
public class ProjectTemplateQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板名称（模糊）")
    private String keyword;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
