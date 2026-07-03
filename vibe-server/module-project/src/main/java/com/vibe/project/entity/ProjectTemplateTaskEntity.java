package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 项目模板任务实体（project_template_task）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_template_task")
@Schema(description = "项目模板任务")
public class ProjectTemplateTaskEntity extends ProjectBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

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
