package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务转派 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "任务转派")
public class TaskReassignDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目任务ID不能为空")
    private Long taskId;

    @Schema(description = "新工程师ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新工程师ID不能为空")
    private Long newEngineerId;

    @Schema(description = "新开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "新结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "转派原因")
    @Size(max = 255, message = "转派原因长度不能超过255")
    private String reason;

    @Schema(description = "是否跳过冲突检测")
    private Boolean ignoreConflict;
}
