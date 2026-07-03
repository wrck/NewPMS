package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 仓库实体（warehouse）。
 *
 * <p>safety_stock 为 JSON 字符串，按型号键值对存储各型号安全库存阈值。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warehouse")
@Schema(description = "仓库")
public class WarehouseEntity extends DeviceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "仓库编码")
    private String warehouseCode;

    @Schema(description = "仓库地址")
    private String address;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "仓库管理员ID")
    private Long managerId;

    @Schema(description = "安全库存配置（JSON 字符串，按型号键值对）")
    private String safetyStock;
}
