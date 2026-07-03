package com.vibe.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 备件新增/编辑 DTO。
 *
 * @author vibe
 */
@Data
@Schema(description = "备件新增/编辑")
public class SparePartDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "备件ID（编辑时必填）")
    private Long id;

    @Schema(description = "备件名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "备件名称不能为空")
    @Size(max = 128, message = "备件名称长度不能超过128")
    private String partName;

    @Schema(description = "备件编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "备件编码不能为空")
    @Size(max = 64, message = "备件编码长度不能超过64")
    private String partCode;

    @Schema(description = "关联设备型号ID")
    private Long modelId;

    @Schema(description = "所属仓库ID")
    private Long warehouseId;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "库存数量")
    @Min(value = 0, message = "库存数量不能为负")
    private Integer quantity;
}
