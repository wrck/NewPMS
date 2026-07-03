package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目模板阶段 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目模板阶段")
public class ProjectTemplatePhaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "阶段ID（编辑时必填）")
    private Long id;

    @Schema(description = "项目模板ID")
    private Long templateId;

    @Schema(description = "阶段编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "阶段编码不能为空")
    @Size(max = 32, message = "阶段编码长度不能超过32")
    private String phaseCode;

    @Schema(description = "阶段名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "阶段名称不能为空")
    @Size(max = 64, message = "阶段名称长度不能超过64")
    private String phaseName;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "交付物清单（JSON 字符串）")
    private String deliverables;
}
