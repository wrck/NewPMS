package com.vibe.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 设备实例单条录入/编辑 DTO。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备实例录入/编辑")
public class DeviceInstanceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID（编辑时必填）")
    private Long id;

    @Schema(description = "序列号 SN（全局唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "设备序列号 SN 不能为空")
    @Size(max = 64, message = "SN 长度不能超过64")
    private String serialNumber;

    @Schema(description = "MAC 地址")
    @Size(max = 32, message = "MAC 地址长度不能超过32")
    private String macAddress;

    @Schema(description = "设备型号ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备型号ID不能为空")
    private Long modelId;

    @Schema(description = "固件版本")
    @Size(max = 64, message = "固件版本长度不能超过64")
    private String firmwareVersion;

    @Schema(description = "所属项目ID")
    private Long projectId;

    @Schema(description = "关联项目阶段ID")
    private Long phaseId;

    @Schema(description = "安装站点名称")
    @Size(max = 128, message = "安装站点名称长度不能超过128")
    private String siteName;

    @Schema(description = "安装位置（机房-机柜-层位）")
    @Size(max = 255, message = "安装位置长度不能超过255")
    private String location;

    @Schema(description = "所属仓库ID")
    private Long warehouseId;

    @Schema(description = "当前保管代理商ID")
    private Long agentCompanyId;

    @Schema(description = "设备状态")
    private String status;

    @Schema(description = "安装日期")
    private LocalDate installDate;

    @Schema(description = "安装人员ID")
    private Long installerId;

    @Schema(description = "备注")
    private String remark;
}
