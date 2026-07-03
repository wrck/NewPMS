package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 任务列表项 VO
 *
 * <p>PM 首页待派单任务、工程师首页今日任务/超期任务共用。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "任务列表项")
public class TaskItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "执行模式 SELF/AGENT")
    private String executeMode;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEnd;

    @Schema(description = "是否超期（1-是 0-否）")
    private Integer overdue;
}
