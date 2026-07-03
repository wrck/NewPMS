package com.vibe.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 交付物审核 DTO
 *
 * <p>PM 审核交付物：</p>
 * <ul>
 *   <li>approved = true：审核通过，任务状态 SUBMITTED → CONFIRMED</li>
 *   <li>approved = false：审核退回，任务状态 SUBMITTED → RETURNED → IN_PROGRESS，
 *       submit_count +1，填写退回原因</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "交付物审核")
public class DeliverableReviewDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否审核通过", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Schema(description = "退回原因（审核退回时必填）")
    private String rejectReason;

    @Schema(description = "审核评语")
    private String reviewComment;
}
