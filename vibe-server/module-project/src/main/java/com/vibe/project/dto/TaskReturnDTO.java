package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "退回原因")
    private String reason;
}
