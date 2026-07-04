package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 财务报表 VO
 *
 * <p>对齐前端 {@code report.ts -> getFinanceReport} 返回结构。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "财务报表")
public class FinanceReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "汇总")
    private Summary summary;

    @Schema(description = "客户维度利润")
    private List<CustomerProfit> byCustomer;

    @Schema(description = "区域维度利润")
    private List<RegionProfit> byRegion;

    @Schema(description = "产品线维度利润")
    private List<ProductLineProfit> byProductLine;

    @Schema(description = "代理商结算汇总")
    private List<AgentSettlement> agentSettlement;

    @Data
    @Schema(description = "财务汇总")
    public static class Summary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "总收入") private BigDecimal totalRevenue;
        @Schema(description = "总成本") private BigDecimal totalCost;
        @Schema(description = "总利润") private BigDecimal totalProfit;
        @Schema(description = "利润率（百分比）") private BigDecimal profitMargin;
    }

    @Data
    @Schema(description = "客户利润")
    public static class CustomerProfit implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "客户ID") private Long customerId;
        @Schema(description = "客户名称") private String customerName;
        @Schema(description = "收入") private BigDecimal revenue;
        @Schema(description = "成本") private BigDecimal cost;
        @Schema(description = "利润") private BigDecimal profit;
    }

    @Data
    @Schema(description = "区域利润")
    public static class RegionProfit implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "区域") private String region;
        @Schema(description = "收入") private BigDecimal revenue;
        @Schema(description = "成本") private BigDecimal cost;
        @Schema(description = "利润") private BigDecimal profit;
    }

    @Data
    @Schema(description = "产品线利润")
    public static class ProductLineProfit implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "产品线") private String productLine;
        @Schema(description = "收入") private BigDecimal revenue;
        @Schema(description = "成本") private BigDecimal cost;
        @Schema(description = "利润") private BigDecimal profit;
    }

    @Data
    @Schema(description = "代理商结算")
    public static class AgentSettlement implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "代理商ID") private Long agentCompanyId;
        @Schema(description = "代理商名称") private String agentCompanyName;
        @Schema(description = "结算总额") private BigDecimal totalAmount;
        @Schema(description = "已付金额") private BigDecimal paidAmount;
        @Schema(description = "待付金额") private BigDecimal pendingAmount;
    }
}
