package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 项目设备清单实体（device_bom）。
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_bom")
@Schema(description = "项目设备清单")
public class DeviceBomEntity extends DeviceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "设备型号ID")
    private Long modelId;

    @Schema(description = "计划数量")
    private Integer plannedQty;

    @Schema(description = "已到货数量")
    private Integer receivedQty;

    @Schema(description = "已安装数量")
    private Integer installedQty;

    @Schema(description = "已验收数量")
    private Integer acceptedQty;

    @Schema(description = "备注")
    private String remark;
}
