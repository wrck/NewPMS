package com.vibe.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 备件操作 DTO（入库/领用/归还/返修）。
 *
 * @author vibe
 */
@Data
@Schema(description = "备件操作")
public class SparePartActionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "备件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "备件ID不能为空")
    private Long sparePartId;

    @Schema(description = "操作类型 IN/OUT/RETURN/REPAIR", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作类型不能为空")
    private String actionType;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "备注")
    private String remark;
}
