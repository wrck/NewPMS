package com.vibe.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 集成配置新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "集成配置新增/编辑")
public class IntegrationConfigSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID（编辑时必填）")
    private Long id;

    @Schema(description = "系统编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "系统编码不能为空")
    @Size(max = 64, message = "系统编码长度不能超过64")
    private String systemCode;

    @Schema(description = "系统名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "系统名称不能为空")
    @Size(max = 128, message = "系统名称长度不能超过128")
    private String systemName;

    @Schema(description = "适配器类型")
    private String adapterType;

    @Schema(description = "接入点 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "接入点 URL 不能为空")
    @Size(max = 512, message = "URL 长度不能超过512")
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
}
