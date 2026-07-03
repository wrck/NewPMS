package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目模板任务 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目模板任务")
public class ProjectTemplateTaskDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID（编辑时必填）")
    private Long id;

    @Schema(description = "项目模板ID")
    private Long templateId;

    @Schema(description = "所属阶段编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "所属阶段编码不能为空")
    private String phaseCode;

    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称长度不能超过128")
    private String taskName;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务描述")
    @Size(max = 512, message = "任务描述长度不能超过512")
    private String description;

    @Schema(description = "默认工期（天）")
    private Integer defaultDays;
}
