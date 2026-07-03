package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 备件实体（spare_part）。
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spare_part")
@Schema(description = "备件")
public class SparePartEntity extends DeviceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "备件名称")
    private String partName;

    @Schema(description = "备件编码")
    private String partCode;

    @Schema(description = "关联设备型号ID")
    private Long modelId;

    @Schema(description = "所属仓库ID")
    private Long warehouseId;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "库存数量")
    private Integer quantity;
}
