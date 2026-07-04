package com.vibe.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 利润分析 VO（项目级利润 / 毛利率）
 *
 * @author vibe
 */
@Data
@Schema(description = "利润分析")
public class FinanceProfitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "合同收入")
    private BigDecimal revenue;

    @Schema(description = "自有成本（人工+差旅+其他）")
    private BigDecimal selfCost;

    @Schema(description = "代理商成本")
    private BigDecimal agentCost;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "毛利润 = 收入 - 总成本")
    private BigDecimal profit;

    @Schema(description = "毛利率（百分比，0-100）")
    private BigDecimal profitMargin;

    @Schema(description = "自施成本占比（百分比，0-100）")
    private BigDecimal selfCostRatio;

    @Schema(description = "代施成本占比（百分比，0-100）")
    private BigDecimal agentCostRatio;
}
