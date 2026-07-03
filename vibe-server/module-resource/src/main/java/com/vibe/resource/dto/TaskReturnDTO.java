package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务退回 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "任务退回")
public class TaskReturnDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目任务ID不能为空")
    private Long taskId;

    @Schema(description = "退回原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 512, message = "退回原因长度不能超过512")
    private String reason;
}
