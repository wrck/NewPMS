package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理驾驶舱核心指标 VO
 *
 * <p>展示项目/设备/工程师/代理商四大核心指标，含本月与上月数据及环比增长率。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "驾驶舱核心指标")
public class CockpitStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /* ============ 项目指标 ============ */
    @Schema(description = "项目总数")
    private Long projectCount;

    @Schema(description = "进行中项目数（非终态）")
    private Long activeProjectCount;

    @Schema(description = "上月项目总数（环比基准）")
    private Long lastMonthProjectCount;

    @Schema(description = "项目环比增长率（百分比，如 12.5 表示增长 12.5%）")
    private Double projectGrowthRate;

    /* ============ 设备指标 ============ */
    @Schema(description = "设备总数")
    private Long deviceCount;

    @Schema(description = "在网设备数（状态 ONLINE）")
    private Long onlineDeviceCount;

    @Schema(description = "上月设备总数")
    private Long lastMonthDeviceCount;

    @Schema(description = "设备环比增长率")
    private Double deviceGrowthRate;

    /* ============ 工程师指标 ============ */
    @Schema(description = "工程师总数")
    private Long engineerCount;

    @Schema(description = "在职工程师数（状态 ACTIVE）")
    private Long activeEngineerCount;

    @Schema(description = "上月工程师总数")
    private Long lastMonthEngineerCount;

    @Schema(description = "工程师环比增长率")
    private Double engineerGrowthRate;

    /* ============ 代理商指标 ============ */
    @Schema(description = "代理商总数")
    private Long agentCompanyCount;

    @Schema(description = "活跃代理商数（状态 ACTIVE）")
    private Long activeAgentCount;

    @Schema(description = "上月代理商总数")
    private Long lastMonthAgentCount;

    @Schema(description = "代理商环比增长率")
    private Double agentGrowthRate;
}
