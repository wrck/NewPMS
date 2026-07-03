package com.vibe.device.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 设备实例分页查询 DTO（按 SN/MAC/状态/项目/仓库筛选）。
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备实例查询")
public class DeviceInstanceQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字（SN / MAC 模糊匹配）")
    private String keyword;

    @Schema(description = "设备状态")
    private String status;

    @Schema(description = "设备型号ID")
    private Long modelId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "安装站点名称（模糊）")
    private String siteName;
}
