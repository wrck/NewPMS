package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备件视图对象（含仓库名/型号名）。
 *
 * @author vibe
 */
@Data
@Schema(description = "备件")
public class SparePartVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "备件ID")
    private Long id;

    @Schema(description = "备件名称")
    private String partName;

    @Schema(description = "备件编码")
    private String partCode;

    @Schema(description = "关联设备型号ID")
    private Long modelId;

    @Schema(description = "型号名称")
    private String modelName;

    @Schema(description = "所属仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "库存数量")
    private Integer quantity;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
