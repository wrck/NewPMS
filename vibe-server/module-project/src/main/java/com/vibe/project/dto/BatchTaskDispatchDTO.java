package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class BatchTaskDispatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "任务ID列表不能为空")
    private List<Long> taskIds;

    @Schema(description = "派发信息（统一应用到全部任务）")
    private TaskDispatchDTO dispatch;
}
