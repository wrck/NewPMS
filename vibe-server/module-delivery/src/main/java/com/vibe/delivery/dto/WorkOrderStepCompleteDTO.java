package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 施工步骤完成 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "施工步骤完成")
public class WorkOrderStepCompleteDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "步骤状态 PENDING/IN_PROGRESS/COMPLETED/SKIPPED")
    private String status;

    @Schema(description = "实际耗时（分钟）")
    private Integer actualMinutes;

    @Schema(description = "备注")
    private String remark;
}
