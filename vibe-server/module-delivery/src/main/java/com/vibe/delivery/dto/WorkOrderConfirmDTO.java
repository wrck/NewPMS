package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单完成确认 DTO（PM 确认时使用）
 *
 * @author vibe
 */
@Data
@Schema(description = "工单完成确认")
public class WorkOrderConfirmDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "确认备注")
    private String remark;
}
