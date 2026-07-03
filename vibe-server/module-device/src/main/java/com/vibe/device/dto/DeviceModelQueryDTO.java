package com.vibe.device.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 设备型号分页查询 DTO。
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备型号查询")
public class DeviceModelQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字（型号名/编码/厂商）")
    private String keyword;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "设备类别")
    private String category;
}
