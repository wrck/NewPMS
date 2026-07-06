package com.vibe.lowcode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 低代码配置实例化 DTO（基于模板实例化新配置）
 *
 * @author vibe
 */
@Data
@Schema(description = "低代码配置实例化")
public class LowcodeInstantiateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "配置名称不能为空")
    @Schema(description = "新配置名称")
    private String configName;

    @Schema(description = "新配置编码（不传则自动生成）")
    private String configCode;
}
