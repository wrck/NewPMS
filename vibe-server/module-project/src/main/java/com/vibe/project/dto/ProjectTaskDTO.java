package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目任务新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目任务新增/编辑")
public class ProjectTaskDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID（编辑时必填）")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "所属阶段ID")
    private Long phaseId;

    @Schema(description = "父任务ID")
    private Long parentTaskId;

    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称长度不能超过128")
    private String taskName;

    @Schema(description = "任务类型 SURVEY/INSTALL/DEBUG/CUTOVER/ACCEPT/OTHER")
    private String taskType;

    @Schema(description = "执行模式 SELF/AGENT")
    private String executeMode;

    @Schema(description = "优先级 HIGH/MEDIUM/LOW")
    private String priority;

    @Schema(description = "计划开始")
    private LocalDate plannedStart;

    @Schema(description = "计划结束")
    private LocalDate plannedEnd;

    @Schema(description = "关联站点信息（JSON 字符串）")
    private String siteInfo;

    @Schema(description = "关联设备ID列表（JSON 字符串）")
    private String deviceIds;

    @Schema(description = "任务描述与要求")
    private String description;

    @Schema(description = "附件列表（JSON 字符串）")
    private String attachments;
}
