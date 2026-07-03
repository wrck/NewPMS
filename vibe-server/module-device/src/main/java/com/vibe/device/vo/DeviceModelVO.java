package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备型号视图对象。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备型号")
public class DeviceModelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "型号ID")
    private Long id;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "型号名称")
    private String modelName;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "厂商")
    private String vendor;

    @Schema(description = "设备类别")
    private String category;

    @Schema(description = "技术规格（JSON 字符串）")
    private String specifications;

    @Schema(description = "默认配置模板")
    private String configTemplate;

    @Schema(description = "安装手册链接")
    private String manualUrl;

    @Schema(description = "产品图片")
    private String imageUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
