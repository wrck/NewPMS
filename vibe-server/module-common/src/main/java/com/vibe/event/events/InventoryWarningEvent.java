package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 库存预警事件
 *
 * <p>触发时机：设备库存低于安全库存阈值，或某型号库存异常时发布。
 * 下游消费者：通知引擎（推送库存预警）、BI 统计、ES 同步。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "库存预警事件")
public class InventoryWarningEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备型号ID")
    private Long modelId;

    @Schema(description = "设备型号名称")
    private String modelName;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "当前库存数量")
    private Integer currentStock;

    @Schema(description = "安全库存阈值")
    private Integer safetyStock;

    @Schema(description = "预警级别：LOW/CRITICAL")
    private String level;

    public InventoryWarningEvent() {
        super(DomainEventConstant.EVENT_INVENTORY_WARNING, null);
    }

    public InventoryWarningEvent(Long modelId, String modelName, Long warehouseId, String warehouseName,
                                  Integer currentStock, Integer safetyStock, String level) {
        super(DomainEventConstant.EVENT_INVENTORY_WARNING, String.valueOf(modelId));
        this.modelId = modelId;
        this.modelName = modelName;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.currentStock = currentStock;
        this.safetyStock = safetyStock;
        this.level = level;
    }
}
