package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 设备状态变更事件
 *
 * <p>触发时机：设备状态流转（IN_FACTORY→IN_STOCK→SHIPPED→INSTALLED→ONLINE→FAULTY→SCRAPPED）后发布。
 * 下游消费者：ES 同步（更新 vibe_device.status）、库存预警、通知引擎。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备状态变更事件")
public class DeviceStatusChangedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备SN")
    private String sn;

    @Schema(description = "原状态")
    private String fromStatus;

    @Schema(description = "新状态")
    private String toStatus;

    @Schema(description = "所属项目ID")
    private Long projectId;

    public DeviceStatusChangedEvent() {
        super(DomainEventConstant.EVENT_DEVICE_STATUS_CHANGED, null);
    }

    public DeviceStatusChangedEvent(Long deviceId, String sn, String fromStatus, String toStatus, Long projectId) {
        super(DomainEventConstant.EVENT_DEVICE_STATUS_CHANGED, String.valueOf(deviceId));
        this.deviceId = deviceId;
        this.sn = sn;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.projectId = projectId;
    }
}
