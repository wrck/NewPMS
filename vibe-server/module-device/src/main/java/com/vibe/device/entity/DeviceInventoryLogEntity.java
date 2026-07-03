package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 设备出入库记录实体（device_inventory_log）。
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_inventory_log")
@Schema(description = "设备出入库记录")
public class DeviceInventoryLogEntity extends DeviceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备实例ID")
    private Long deviceId;

    @Schema(description = "操作类型 IN/OUT/RETURN/TRANSFER")
    private String actionType;

    @Schema(description = "调出仓库ID")
    private Long fromWarehouseId;

    @Schema(description = "调入仓库ID")
    private Long toWarehouseId;

    @Schema(description = "调出项目ID")
    private Long fromProjectId;

    @Schema(description = "调入项目ID")
    private Long toProjectId;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "备注")
    private String remark;
}
