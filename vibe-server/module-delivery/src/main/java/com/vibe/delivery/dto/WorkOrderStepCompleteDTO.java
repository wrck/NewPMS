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

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否跳过（true 时状态置为 SKIPPED）")
    private Boolean skipped;
}
