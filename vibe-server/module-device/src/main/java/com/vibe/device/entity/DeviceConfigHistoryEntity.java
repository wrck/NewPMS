package com.vibe.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 设备配置历史实体（device_config_history）。
 *
 * <p>注意：本表的 version 列为「配置版本号」（业务字段），非乐观锁，
 * 故继承 {@link DeviceBaseEntity}（无 @Version）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_config_history")
@Schema(description = "设备配置历史")
public class DeviceConfigHistoryEntity extends DeviceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备实例ID")
    private Long deviceId;

    @Schema(description = "配置版本号（业务字段）")
    private Integer version;

    @Schema(description = "配置文件地址")
    private String configFileUrl;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "变更说明")
    private String changeDesc;
}
