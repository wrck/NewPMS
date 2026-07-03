package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库存预警视图对象（某仓库某型号库存低于安全库存）。
 *
 * @author vibe
 */
@Data
@Schema(description = "库存预警")
public class InventoryWarningVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "设备型号ID")
    private Long modelId;

    @Schema(description = "型号名称")
    private String modelName;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "当前在库数量")
    private long currentQty;

    @Schema(description = "安全库存阈值")
    private long safetyQty;

    @Schema(description = "缺口数量（安全库存 - 当前在库）")
    private long gapQty;
}
