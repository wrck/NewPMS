package com.vibe.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仓库新增/编辑 DTO。
 *
 * @author vibe
 */
@Data
@Schema(description = "仓库新增/编辑")
public class WarehouseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库ID（编辑时必填）")
    private Long id;

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 128, message = "仓库名称长度不能超过128")
    private String warehouseName;

    @Schema(description = "仓库编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 64, message = "仓库编码长度不能超过64")
    private String warehouseCode;

    @Schema(description = "仓库地址")
    @Size(max = 255, message = "仓库地址长度不能超过255")
    private String address;

    @Schema(description = "区域")
    @Size(max = 32, message = "区域长度不能超过32")
    private String region;

    @Schema(description = "仓库管理员ID")
    private Long managerId;

    @Schema(description = "安全库存配置（JSON 字符串，按型号键值对，如 {\"1001\":5,\"1002\":3}）")
    private String safetyStock;
}
