package com.vibe.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理商结算 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "代理商结算")
public class FinanceWorkloadConfirmationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联转包任务ID")
    private Long outsourceTaskId;

    @Schema(description = "代理商ID")
    private Long agentCompanyId;

    @Schema(description = "对账周期")
    private String period;

    @Schema(description = "工作量（人天）")
    private BigDecimal workloadDays;

    @Schema(description = "人天单价")
    private BigDecimal unitPrice;

    @Schema(description = "差旅费用")
    private BigDecimal travelAmount;

    @Schema(description = "其他费用")
    private BigDecimal otherAmount;

    @Schema(description = "结算总额")
    private BigDecimal totalAmount;

    @Schema(description = "PM 确认人ID")
    private Long pmConfirmUserId;

    @Schema(description = "PM 确认时间")
    private LocalDateTime pmConfirmTime;

    @Schema(description = "代理商确认人ID")
    private Long agentConfirmUserId;

    @Schema(description = "代理商确认时间")
    private LocalDateTime agentConfirmTime;

    @Schema(description = "审批状态")
    private String approvalStatus;

    @Schema(description = "付款状态 UNPAID/PAYING/PAID")
    private String paymentStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
