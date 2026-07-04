package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 管理驾驶舱聚合数据 VO
 *
 * <p>一次性返回驾驶舱所需的全部数据，字段名对齐前端 {@code CockpitData} 接口：
 * KPI、项目阶段分布、项目趋势、风险预警、待办事项、最近动态。</p>
 *
 * <p>对应前端 {@code getCockpit()} 一次调用获取全部数据的需求。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "管理驾驶舱聚合数据")
public class CockpitAggregatedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 核心指标 KPI */
    @Schema(description = "核心指标")
    private CockpitKpiVO kpi;

    /** 项目阶段分布（饼图） */
    @Schema(description = "项目阶段分布")
    private List<PhaseDistributionVO> phaseDistribution;

    /** 近12月项目趋势（折线图） */
    @Schema(description = "项目趋势")
    private List<ProjectTrendVO> projectTrend;

    /** 风险预警列表 */
    @Schema(description = "风险预警")
    private List<RiskWarningVO> riskWarnings;

    /** 待办事项（暂为空列表，后续接入审批/任务模块） */
    @Schema(description = "待办事项")
    private List<Object> todoList = Collections.emptyList();

    /** 最近动态（暂为空列表，后续接入操作日志） */
    @Schema(description = "最近动态")
    private List<Object> recentActivities = Collections.emptyList();
}
