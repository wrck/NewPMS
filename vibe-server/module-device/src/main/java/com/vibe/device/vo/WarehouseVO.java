package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 仓库视图对象（含管理员名）。
 *
 * @author vibe
 */
@Data
@Schema(description = "仓库")
public class WarehouseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库ID")
    private Long id;

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

    @Schema(description = "仓库管理员姓名")
    private String managerName;

    @Schema(description = "安全库存配置（JSON 字符串，按型号键值对）")
    private String safetyStock;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
