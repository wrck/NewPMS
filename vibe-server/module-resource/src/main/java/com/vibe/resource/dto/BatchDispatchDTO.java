package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量派单 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "批量派单")
public class BatchDispatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "派单列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "派单列表不能为空")
    @Valid
    private List<TaskDispatchDTO> dispatches;
}
