package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 割接步骤执行 DTO
 *
 * <p>用于步骤开始/完成/回退/异常等操作。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "割接步骤执行")
public class CutoverStepExecuteDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "割接方案ID不能为空")
    @Schema(description = "割接方案ID")
    private Long planId;

    @NotNull(message = "步骤ID不能为空")
    @Schema(description = "步骤ID")
    private Long stepId;

    @Schema(description = "执行备注")
    private String executionRemark;

    @Schema(description = "异常说明（异常时使用）")
    private String exceptionRemark;

    @Schema(description = "实际耗时（分钟，完成时使用）")
    private Integer actualDuration;
}
