package com.vibe.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集成配置视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "集成配置")
public class IntegrationConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "系统编码")
    private String systemCode;

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "适配器类型")
    private String adapterType;

    @Schema(description = "接入点 URL")
    private String endpointUrl;

    @Schema(description = "认证方式")
    private String authType;

    @Schema(description = "认证配置（JSON）")
    private String authConfig;

    @Schema(description = "超时毫秒")
    private Integer timeoutMs;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "是否启用 1-是 0-否")
    private Integer enabled;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "最近调用时间")
    private LocalDateTime lastCallTime;

    @Schema(description = "最近调用状态")
    private String lastCallStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
