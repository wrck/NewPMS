package com.vibe.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目预算实体（finance_budget 表，含 @Version 乐观锁）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_budget")
@Schema(description = "项目预算")
public class FinanceBudgetEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

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

    @Schema(description = "预算总额（冗余字段）")
    private BigDecimal totalAmount;

    @Schema(description = "审批状态 DRAFT/PENDING/APPROVED/REJECTED")
    private String approvalStatus;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "备注")
    private String remark;
}
