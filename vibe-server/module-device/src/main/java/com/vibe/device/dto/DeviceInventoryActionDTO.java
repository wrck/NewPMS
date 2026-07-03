package com.vibe.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备出入库操作 DTO（入库/出库/退库/调拨/盘点）。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备出入库操作")
public class DeviceInventoryActionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    @Schema(description = "操作类型 IN/OUT/RETURN/TRANSFER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作类型不能为空")
    private String actionType;

    @Schema(description = "调出仓库ID（出库/调拨时填写）")
    private Long fromWarehouseId;

    @Schema(description = "调入仓库ID（入库/退库/调拨时填写）")
    private Long toWarehouseId;

    @Schema(description = "调出项目ID（出库/调拨时填写）")
    private Long fromProjectId;

    @Schema(description = "调入项目ID（入库/退库/调拨时填写）")
    private Long toProjectId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    @Schema(description = "备注")
    private String remark;
}
