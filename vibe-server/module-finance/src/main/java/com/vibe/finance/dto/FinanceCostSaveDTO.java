package com.vibe.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成本创建/更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "成本创建/更新")
public class FinanceCostSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（更新时必填）")
    private Long id;

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "关联项目ID")
    private Long projectId;

    @NotBlank(message = "成本类型不能为空")
    @Schema(description = "成本类型 LABOR/TRAVEL/AGENT/OTHER")
    private String costType;

    @NotNull(message = "金额不能为空")
    @Schema(description = "金额")
    private BigDecimal amount;

    @NotNull(message = "发生日期不能为空")
    @Schema(description = "发生日期")
    private LocalDate costDate;

    @Schema(description = "关联业务类型 TIMESHEET/BUSINESS_TRIP/OUTSOURCE_TASK/MANUAL")
    private String refType;

    @Schema(description = "关联业务ID")
    private Long refId;

    @Schema(description = "费用说明")
    private String description;
}
