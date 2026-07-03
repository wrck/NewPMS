package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备出入库记录视图对象（含仓库名/项目名/设备SN）。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备出入库记录")
public class DeviceInventoryLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备序列号")
    private String serialNumber;

    @Schema(description = "操作类型 IN/OUT/RETURN/TRANSFER")
    private String actionType;

    @Schema(description = "调出仓库ID")
    private Long fromWarehouseId;

    @Schema(description = "调出仓库名称")
    private String fromWarehouseName;

    @Schema(description = "调入仓库ID")
    private Long toWarehouseId;

    @Schema(description = "调入仓库名称")
    private String toWarehouseName;

    @Schema(description = "调出项目ID")
    private Long fromProjectId;

    @Schema(description = "调出项目名称")
    private String fromProjectName;

    @Schema(description = "调入项目ID")
    private Long toProjectId;

    @Schema(description = "调入项目名称")
    private String toProjectName;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
