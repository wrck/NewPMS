package com.vibe.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工作量提交 DTO
 *
 * <p>代理商提交工作量（人天/站点数/设备台数），由 PM 确认。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "工作量提交")
public class OutsourceWorkloadDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "转包任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "转包任务ID不能为空")
    private Long outsourceTaskId;

    @Schema(description = "人天")
    private BigDecimal manDays;

    @Schema(description = "站点数")
    private Integer siteCount;

    @Schema(description = "设备台数")
    private Integer deviceCount;

    @Schema(description = "差旅天数")
    private Integer travelDays;

    @Schema(description = "其他费用")
    private BigDecimal otherCost;

    @Schema(description = "结算总额")
    private BigDecimal totalAmount;

    @Schema(description = "备注")
    private String remark;
}
