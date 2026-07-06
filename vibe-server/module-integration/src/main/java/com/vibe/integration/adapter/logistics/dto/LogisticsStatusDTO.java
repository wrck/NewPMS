package com.vibe.integration.adapter.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流状态 DTO
 *
 * <p>用于 {@code LogisticsStatusFeignClient} 拉取物流状态，
 * 更新设备 SHIPPED 阶段的预计到货时间。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "物流状态")
public class LogisticsStatusDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "运单号")
    private String trackingNo;

    @Schema(description = "物流公司编码（如 SF/EMS/YT）")
    private String carrierCode;

    @Schema(description = "物流公司名称")
    private String carrierName;

    @Schema(description = "物流状态（PENDING/IN_TRANSIT/DELIVERED/EXCEPTION/RETURNED）")
    private String status;

    @Schema(description = "当前节点城市")
    private String currentCity;

    @Schema(description = "最新节点描述")
    private String lastNodeDesc;

    @Schema(description = "最新节点时间")
    private LocalDateTime lastNodeTime;

    @Schema(description = "预计到货时间")
    private LocalDateTime estimatedDeliveryTime;

    @Schema(description = "实际签收时间")
    private LocalDateTime actualDeliveryTime;

    @Schema(description = "签收人")
    private String signedBy;

    @Schema(description = "异常原因（status=EXCEPTION 时填充）")
    private String exceptionReason;

    @Schema(description = "源单据号（本系统出库单号）")
    private String sourceOrderNo;

    @Schema(description = "拉取时间（本系统填充）")
    private LocalDateTime pulledAt;
}
