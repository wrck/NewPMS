package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 总监首页 VO
 *
 * <p>全局总览 + 审批待办 + 核心指标卡片 + 项目趋势 + 风险项目。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "总监首页")
public class DirectorDashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "核心指标卡片（含环比）")
    private CockpitStatVO stats;

    @Schema(description = "待审批数（项目变更 PENDING + 工作量 SUBMITTED）")
    private Long pendingApprovalCount;

    @Schema(description = "待审批变更记录数")
    private Long pendingChangeCount;

    @Schema(description = "待确认工作量数")
    private Long pendingWorkloadCount;

    @Schema(description = "项目状态分布（饼图）")
    private List<ChartDataVO> projectStatusDist;

    @Schema(description = "近12月项目趋势（折线图）")
    private List<ChartDataVO> projectTrend;

    @Schema(description = "风险项目列表（Top N）")
    private List<RiskProjectVO> riskProjects;
}
