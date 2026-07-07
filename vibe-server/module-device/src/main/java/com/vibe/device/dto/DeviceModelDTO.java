package com.vibe.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备型号新增/编辑 DTO。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备型号新增/编辑")
public class DeviceModelDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "型号ID（编辑时必填）")
    private Long id;

    @Schema(description = "型号编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "型号编码不能为空")
    @Size(max = 64, message = "型号编码长度不能超过64")
    private String modelCode;

    @Schema(description = "型号名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "型号名称不能为空")
    @Size(max = 128, message = "型号名称长度不能超过128")
    private String modelName;

    @Schema(description = "产品线 路由/交换/无线/安全/数据中心/其他")
    @Size(max = 32, message = "产品线长度不能超过32")
    private String productLine;

    @Schema(description = "厂商")
    @Size(max = 64, message = "厂商长度不能超过64")
    private String vendor;

    @Schema(description = "设备类别 ROUTER/SWITCH/AP/FIREWALL/WLC/LB/OTHER")
    @Size(max = 32, message = "设备类别长度不能超过32")
    private String category;

    @Schema(description = "技术规格（JSON 字符串，键值对）")
    private String specifications;

    @Schema(description = "默认配置模板")
    private String configTemplate;

    @Schema(description = "安装手册链接")
    @Size(max = 255, message = "安装手册链接长度不能超过255")
    private String manualUrl;

    @Schema(description = "产品图片")
    @Size(max = 255, message = "产品图片链接长度不能超过255")
    private String imageUrl;

    @Schema(description = "描述")
    @Size(max = 512, message = "描述长度不能超过512")
    private String description;
}
