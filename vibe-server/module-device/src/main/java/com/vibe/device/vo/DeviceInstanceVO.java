package com.vibe.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备实例视图对象（含型号名/项目名/仓库名）。
 *
 * @author vibe
 */
@Data
@Schema(description = "设备实例")
public class DeviceInstanceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "序列号 SN")
    private String serialNumber;

    @Schema(description = "MAC 地址")
    private String macAddress;

    @Schema(description = "设备型号ID")
    private Long modelId;

    @Schema(description = "型号名称")
    private String modelName;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "所属项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "关联项目阶段ID")
    private Long phaseId;

    @Schema(description = "安装站点名称")
    private String siteName;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "设备类别")
    private String category;

    @Schema(description = "设备状态")
    private String status;

    @Schema(description = "所属仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "当前保管代理商ID")
    private Long agentCompanyId;

    @Schema(description = "当前配置文件地址")
    private String configFileUrl;

    @Schema(description = "配置版本号")
    private Integer configVersion;

    @Schema(description = "安装日期")
    private LocalDate installedAt;

    @Schema(description = "入网日期")
    private LocalDate onlineAt;

    @Schema(description = "安装人员ID")
    private Long installerId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
