package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务派发 DTO（手动指派）
 *
 * @author vibe
 */
@Data
@Schema(description = "任务手动指派")
public class TaskDispatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目任务ID不能为空")
    private Long taskId;

    @Schema(description = "工程师ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工程师ID不能为空")
    private Long engineerId;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @Schema(description = "是否跳过冲突检测（紧急调配时为 true）")
    private Boolean ignoreConflict;
}
