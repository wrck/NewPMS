package com.vibe.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理商结算创建/更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "代理商结算创建/更新")
public class FinanceWorkloadSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（更新时必填）")
    private Long id;

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联转包任务ID")
    private Long outsourceTaskId;

    @NotNull(message = "代理商ID不能为空")
    @Schema(description = "代理商ID")
    private Long agentCompanyId;

    @NotBlank(message = "对账周期不能为空")
    @Schema(description = "对账周期 YYYY-MM 或 PROJECT")
    private String period;

    @NotNull(message = "工作量不能为空")
    @Schema(description = "工作量（人天）")
    private BigDecimal workloadDays;

    @NotNull(message = "人天单价不能为空")
    @Schema(description = "人天单价")
    private BigDecimal unitPrice;

    @Schema(description = "差旅费用")
    private BigDecimal travelAmount;

    @Schema(description = "其他费用")
    private BigDecimal otherAmount;

    @Schema(description = "备注")
    private String remark;
}
