package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务进度更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "任务进度更新")
public class TaskProgressDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标状态 PENDING/ASSIGNED/IN_PROGRESS/COMPLETED/CONFIRMED",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    @Schema(description = "备注")
    private String remark;
}
