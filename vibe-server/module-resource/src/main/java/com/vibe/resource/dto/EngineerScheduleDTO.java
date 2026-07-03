package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工程师排期 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师排期")
public class EngineerScheduleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "排期ID（编辑时必填）")
    private Long id;

    @Schema(description = "工程师ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工程师ID不能为空")
    private Long engineerId;

    @Schema(description = "关联任务ID（TASK 类型时必填）")
    private Long taskId;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "排期类型 TASK/LEAVE/TRAINING/MEETING")
    private String scheduleType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否跳过冲突检测（紧急调配时为 true）")
    private Boolean ignoreConflict;
}
