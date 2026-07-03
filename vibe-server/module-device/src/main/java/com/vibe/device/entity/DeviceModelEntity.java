package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 设备型号实体（device_model）。
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_model")
@Schema(description = "设备型号")
public class DeviceModelEntity extends DeviceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "型号名称")
    private String modelName;

    @Schema(description = "产品线 路由/交换/无线/安全/数据中心/其他")
    private String productLine;

    @Schema(description = "厂商")
    private String vendor;

    @Schema(description = "设备类别 ROUTER/SWITCH/AP/FIREWALL/WLC/LB/OTHER")
    private String category;

    @Schema(description = "技术规格（JSON 字符串，键值对）")
    private String specifications;

    @Schema(description = "默认配置模板")
    private String configTemplate;

    @Schema(description = "安装手册链接")
    private String manualUrl;

    @Schema(description = "产品图片")
    private String imageUrl;
}
