package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * PM 首页 VO
 *
 * <p>项目概览（我的项目） + 待派单任务 + 待审核交付物。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "PM首页")
public class PmDashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "我的项目总数")
    private Long myProjectCount;

    @Schema(description = "进行中项目数")
    private Long activeProjectCount;

    @Schema(description = "待派单任务数（状态 PENDING）")
    private Long pendingDispatchCount;

    @Schema(description = "待审核交付物数（转包任务状态 SUBMITTED）")
    private Long pendingReviewCount;

    @Schema(description = "风险项目数")
    private Long riskProjectCount;

    @Schema(description = "我的项目列表（Top N）")
    private List<ProjectItemVO> myProjects;

    @Schema(description = "待派单任务列表（Top N）")
    private List<TaskItemVO> pendingDispatchTasks;

    @Schema(description = "待审核交付物列表（Top N）")
    private List<DeliverableItemVO> pendingReviewDeliverables;
}
