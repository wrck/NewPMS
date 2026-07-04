package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理驾驶舱核心指标 KPI VO
 *
 * <p>字段名对齐前端 {@code CockpitKpi} 接口定义，
 * 关注项目执行状态指标（在建/风险/超期/本月新增/结项/利用率/到货率/验收率）。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "驾驶舱核心指标 KPI")
public class CockpitKpiVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 在建项目数 */
    @Schema(description = "在建项目数")
    private Long ongoingProjects;

    /** 风险项目数 */
    @Schema(description = "风险项目数")
    private Long riskProjects;

    /** 超期项目数 */
    @Schema(description = "超期项目数")
    private Long overdueProjects;

    /** 本月新增项目数 */
    @Schema(description = "本月新增项目数")
    private Long monthNewProjects;

    /** 本月结项项目数 */
    @Schema(description = "本月结项项目数")
    private Long monthClosedProjects;

    /** 工程师利用率（百分比） */
    @Schema(description = "工程师利用率（百分比）")
    private Double engineerUtilization;

    /** 设备到货率（百分比） */
    @Schema(description = "设备到货率（百分比）")
    private Double deviceArrivalRate;

    /** 验收完成率（百分比） */
    @Schema(description = "验收完成率（百分比）")
    private Double acceptanceCompletionRate;
}
