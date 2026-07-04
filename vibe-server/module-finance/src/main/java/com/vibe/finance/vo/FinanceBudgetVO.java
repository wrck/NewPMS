package com.vibe.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目预算 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目预算")
public class FinanceBudgetVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "预算年度")
    private Integer year;

    @Schema(description = "人工预算")
    private BigDecimal laborAmount;

    @Schema(description = "差旅预算")
    private BigDecimal travelAmount;

    @Schema(description = "代理商预算")
    private BigDecimal agentAmount;

    @Schema(description = "其他预算")
    private BigDecimal otherAmount;

    @Schema(description = "预算总额")
    private BigDecimal totalAmount;

    @Schema(description = "审批状态 DRAFT/PENDING/APPROVED/REJECTED")
    private String approvalStatus;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ====== 实际成本对比字段（运行时计算） ======

    @Schema(description = "实际人工成本")
    private BigDecimal actualLabor;

    @Schema(description = "实际差旅成本")
    private BigDecimal actualTravel;

    @Schema(description = "实际代理商成本")
    private BigDecimal actualAgent;

    @Schema(description = "实际其他成本")
    private BigDecimal actualOther;

    @Schema(description = "实际总成本")
    private BigDecimal actualTotal;
}
