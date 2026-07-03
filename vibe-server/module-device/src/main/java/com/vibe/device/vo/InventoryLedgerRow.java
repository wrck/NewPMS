package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库存台账聚合行（各仓库各型号在库设备数量）。
 *
 * <p>由 Mapper 聚合查询直接映射，用于库存台账与库存预警。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "库存台账行")
public class InventoryLedgerRow implements Serializable {

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

    @Schema(description = "在库数量（IN_FACTORY）")
    private Long inStockQty;
}
