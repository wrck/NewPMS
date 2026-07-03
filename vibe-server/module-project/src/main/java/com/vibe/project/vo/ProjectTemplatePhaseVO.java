package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目模板阶段视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目模板阶段")
public class ProjectTemplatePhaseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "阶段ID")
    private Long id;

    @Schema(description = "项目模板ID")
    private Long templateId;

    @Schema(description = "阶段编码")
    private String phaseCode;

    @Schema(description = "阶段名称")
    private String phaseName;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "交付物清单")
    private String deliverables;
}
