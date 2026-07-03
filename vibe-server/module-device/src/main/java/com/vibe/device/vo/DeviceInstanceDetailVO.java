package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 设备实例详情视图对象（含状态轨迹与出入库历史）。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备实例详情")
public class DeviceInstanceDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备基础信息")
    private DeviceInstanceVO device;

    @Schema(description = "状态变更轨迹（按时间倒序）")
    private List<DeviceStatusLogVO> statusTrail;

    @Schema(description = "出入库历史（按时间倒序）")
    private List<DeviceInventoryLogVO> inventoryLogs;
}
