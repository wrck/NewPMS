package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 工时审批 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工时审批")
public class TimesheetApprovalDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工时ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工时ID列表不能为空")
    private List<Long> ids;

    @Schema(description = "审批结果 APPROVED/REJECTED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "审批结果不能为空")
    private String decision;

    @Schema(description = "审批意见")
    private String opinion;
}
