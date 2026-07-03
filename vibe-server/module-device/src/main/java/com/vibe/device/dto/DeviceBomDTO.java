package com.vibe.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目设备清单（BOM）维护 DTO。
 *
 * @author vibe
 */
@Data
@Schema(description = "项目设备清单维护")
public class DeviceBomDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "BOM ID（编辑时必填）")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Schema(description = "设备型号ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备型号ID不能为空")
    private Long modelId;

    @Schema(description = "计划数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划数量不能为空")
    @Min(value = 0, message = "计划数量不能为负")
    private Integer plannedQty;

    @Schema(description = "已到货数量")
    @Min(value = 0, message = "已到货数量不能为负")
    private Integer receivedQty;

    @Schema(description = "已安装数量")
    @Min(value = 0, message = "已安装数量不能为负")
    private Integer installedQty;

    @Schema(description = "已验收数量")
    @Min(value = 0, message = "已验收数量不能为负")
    private Integer acceptedQty;

    @Schema(description = "备注")
    private String remark;
}
