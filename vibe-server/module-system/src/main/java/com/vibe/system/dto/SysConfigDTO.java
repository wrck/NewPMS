package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统配置新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "系统配置新增/编辑")
public class SysConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID（编辑时必填）")
    private Long id;

    @Schema(description = "配置名称")
    @Size(max = 128, message = "配置名称长度不能超过128")
    private String configName;

    @Schema(description = "配置键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置键不能为空")
    @Size(max = 128, message = "配置键长度不能超过128")
    private String configKey;

    @Schema(description = "配置值")
    @Size(max = 512, message = "配置值长度不能超过512")
    private String configValue;

    @Schema(description = "配置类型 SYSTEM/CUSTOM")
    private String configType;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
