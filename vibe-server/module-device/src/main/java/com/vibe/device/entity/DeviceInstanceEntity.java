package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 设备实例实体（device_instance）—— 核心表，含乐观锁。
 *
 * <p>继承 {@link com.vibe.common.base.BaseEntity}（含 @Version 乐观锁字段），
 * 对应 schema.sql 中 device_instance.version 列。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_instance")
@Schema(description = "设备实例")
public class DeviceInstanceEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "序列号 SN（全局唯一）")
    private String serialNumber;

    @Schema(description = "MAC 地址")
    private String macAddress;

    @Schema(description = "设备型号ID")
    private Long modelId;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "所属项目ID（未分配时为空）")
    private Long projectId;

    @Schema(description = "关联项目阶段ID")
    private Long phaseId;

    @Schema(description = "安装站点名称")
    private String siteName;

    @Schema(description = "安装位置（机房-机柜-层位）")
    private String installLocation;

    @Schema(description = "设备状态（见状态机）")
    private String status;

    @Schema(description = "所属仓库ID")
    private Long warehouseId;

    @Schema(description = "当前保管代理商ID")
    private Long agentCompanyId;

    @Schema(description = "当前配置文件地址")
    private String configFileUrl;

    @Schema(description = "配置版本号")
    private Integer configVersion;

    @Schema(description = "安装日期")
    private LocalDate installDate;

    @Schema(description = "入网日期")
    private LocalDate onlineDate;

    @Schema(description = "安装人员ID")
    private Long installerId;

    @Schema(description = "备注")
    private String remark;
}
