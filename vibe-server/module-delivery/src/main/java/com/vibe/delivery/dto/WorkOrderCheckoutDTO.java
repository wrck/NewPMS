package com.vibe.delivery.dto;

import com.vibe.delivery.bo.GpsLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单签退 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工单签退")
public class WorkOrderCheckoutDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "GPS 定位信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "GPS 定位信息不能为空")
    private GpsLocation location;

    @Schema(description = "签退备注")
    private String remark;
}
