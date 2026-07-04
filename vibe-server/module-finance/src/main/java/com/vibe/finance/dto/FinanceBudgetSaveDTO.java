package com.vibe.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预算创建/更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "预算创建/更新")
public class FinanceBudgetSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（更新时必填）")
    private Long id;

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "关联项目ID")
    private Long projectId;

    @NotNull(message = "预算年度不能为空")
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

    @Schema(description = "备注")
    private String remark;
}
