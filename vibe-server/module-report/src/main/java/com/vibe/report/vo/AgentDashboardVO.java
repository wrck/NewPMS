package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 代理商首页 VO
 *
 * <p>任务概况（待接单/进行中/待审核） + 各状态任务列表。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "代理商首页")
public class AgentDashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务总数")
    private Long totalCount;

    @Schema(description = "待接单数（状态 PENDING）")
    private Long pendingCount;

    @Schema(description = "进行中数（状态 IN_PROGRESS）")
    private Long inProgressCount;

    @Schema(description = "待审核数（状态 SUBMITTED）")
    private Long submittedCount;

    @Schema(description = "已超期数（状态 OVERDUE）")
    private Long overdueCount;

    @Schema(description = "待接单任务列表（Top N）")
    private List<OutsourceTaskItemVO> pendingTasks;

    @Schema(description = "进行中任务列表（Top N）")
    private List<OutsourceTaskItemVO> inProgressTasks;

    @Schema(description = "待审核任务列表（Top N）")
    private List<OutsourceTaskItemVO> submittedTasks;
}
