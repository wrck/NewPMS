package com.vibe.lowcode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 低代码模板创建/更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "低代码模板创建/更新")
public class LowcodeTemplateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "模板编码不能为空")
    @Schema(description = "模板编码（唯一）")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称")
    private String templateName;

    @NotBlank(message = "模板类型不能为空")
    @Schema(description = "模板类型 FORM/LIST/TAB/RELATION")
    private String templateType;

    @NotBlank(message = "JSON Schema 不能为空")
    @Schema(description = "JSON Schema")
    private String schemaJson;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 1-启用 0-禁用，默认 1")
    private Integer status;
}
