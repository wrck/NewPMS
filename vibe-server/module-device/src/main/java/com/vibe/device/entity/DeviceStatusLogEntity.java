package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 设备状态变更日志实体（device_status_log）。
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_status_log")
@Schema(description = "设备状态变更日志")
public class DeviceStatusLogEntity extends DeviceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备实例ID")
    private Long deviceId;

    @Schema(description = "变更前状态")
    private String fromStatus;

    @Schema(description = "变更后状态")
    private String toStatus;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "备注")
    private String remark;
}
