package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 设备看板视图对象（项目维度完成率、状态分布、异常设备列表、多维统计）。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备看板")
public class DeviceDashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID（指定项目看板时返回，全局看板为 null）")
    private Long projectId;

    @Schema(description = "设备总数")
    private long totalDevices;

    @Schema(description = "BOM 完成率统计（按型号维度的计划/到货/安装/验收数量）")
    private List<DeviceBomVO> bomProgress;

    @Schema(description = "设备状态分布（status → 数量）")
    private List<Map<String, Object>> statusDistribution;

    @Schema(description = "按型号统计（modelId/modelName/modelCode → 数量）")
    private List<Map<String, Object>> countByModel;

    @Schema(description = "异常设备列表（DAMAGED/LOST/REPAIR 状态）")
    private List<DeviceInstanceVO> abnormalDevices;
}
