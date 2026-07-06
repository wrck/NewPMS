package com.vibe.lowcode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 低代码关联页配置创建/更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "低代码关联页配置创建/更新")
public class LowcodeRelationConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "配置编码不能为空")
    @Schema(description = "配置编码（唯一）")
    private String configCode;

    @NotBlank(message = "配置名称不能为空")
    @Schema(description = "配置名称")
    private String configName;

    @NotBlank(message = "JSON Schema 不能为空")
    @Schema(description = "JSON Schema（主从关联/级联规则/显示字段）")
    private String schemaJson;

    @Schema(description = "关联模板ID（lowcode_template.id，可空）")
    private Long templateId;

    @Schema(description = "状态 1-启用 0-禁用，默认 1")
    private Integer status;

    @Schema(description = "描述")
    private String description;
}
