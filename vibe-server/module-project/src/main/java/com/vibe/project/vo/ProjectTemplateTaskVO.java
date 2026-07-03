package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目模板任务视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目模板任务")
public class ProjectTemplateTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "项目模板ID")
    private Long templateId;

    @Schema(description = "所属阶段编码")
    private String phaseCode;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "默认工期（天）")
    private Integer defaultDays;
}
