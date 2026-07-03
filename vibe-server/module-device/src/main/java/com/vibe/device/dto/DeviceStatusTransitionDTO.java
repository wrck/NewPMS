package com.vibe.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备状态流转 DTO。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备状态流转")
public class DeviceStatusTransitionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标状态不能为空")
    private String toStatus;

    @Schema(description = "关联项目ID（出库发运分配项目时必填）")
    private Long projectId;

    @Schema(description = "仓库ID（入库/退库时可选）")
    private Long warehouseId;

    @Schema(description = "安装人员ID（安装完成时可选）")
    private Long installerId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "乐观锁版本号（可选，传入则校验）")
    @NotNull(message = "乐观锁版本号不能为空")
    private Integer version;
}
