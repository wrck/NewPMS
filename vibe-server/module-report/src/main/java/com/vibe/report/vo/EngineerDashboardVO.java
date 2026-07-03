package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 工程师首页 VO
 *
 * <p>今日任务 + 工时统计（今日/本周/本月） + 超期任务。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师首页")
public class EngineerDashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "今日任务数")
    private Long todayTaskCount;

    @Schema(description = "待处理任务数（PENDING/ASSIGNED/IN_PROGRESS）")
    private Long pendingTaskCount;

    @Schema(description = "超期任务数")
    private Long overdueTaskCount;

    @Schema(description = "今日工时（小时）")
    private BigDecimal todayWorkHours;

    @Schema(description = "本周工时（小时）")
    private BigDecimal weekWorkHours;

    @Schema(description = "本月工时（小时）")
    private BigDecimal monthWorkHours;

    @Schema(description = "今日任务列表")
    private List<TaskItemVO> todayTasks;

    @Schema(description = "超期任务列表（Top N）")
    private List<TaskItemVO> overdueTasks;
}
