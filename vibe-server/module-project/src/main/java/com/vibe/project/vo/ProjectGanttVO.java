package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 项目甘特图视图 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目甘特图")
public class ProjectGanttVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "计划开始")
    private LocalDate plannedStart;

    @Schema(description = "计划结束")
    private LocalDate plannedEnd;

    @Schema(description = "进度百分比")
    private Integer progressPct;

    @Schema(description = "阶段甘特数据")
    private List<PhaseGantt> phases;

    @Schema(description = "任务甘特数据")
    private List<TaskGantt> tasks;

    /**
     * 阶段甘特图项
     */
    @Data
    @Schema(description = "阶段甘特图项")
    public static class PhaseGantt implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "阶段ID")
        private Long phaseId;
        @Schema(description = "阶段编码")
        private String phaseCode;
        @Schema(description = "阶段名称")
        private String phaseName;
        @Schema(description = "排序")
        private Integer sortOrder;
        @Schema(description = "状态")
        private String status;
        @Schema(description = "计划开始")
        private LocalDate plannedStart;
        @Schema(description = "计划结束")
        private LocalDate plannedEnd;
    }

    /**
     * 任务甘特图项
     */
    @Data
    @Schema(description = "任务甘特图项")
    public static class TaskGantt implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "任务ID")
        private Long taskId;
        @Schema(description = "任务名称")
        private String taskName;
        @Schema(description = "阶段ID")
        private Long phaseId;
        @Schema(description = "父任务ID")
        private Long parentTaskId;
        @Schema(description = "状态")
        private String status;
        @Schema(description = "执行人ID")
        private Long assigneeId;
        @Schema(description = "执行人姓名")
        private String assigneeName;
        @Schema(description = "计划开始")
        private LocalDate plannedStart;
        @Schema(description = "计划结束")
        private LocalDate plannedEnd;
    }
}
