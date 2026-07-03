package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目模板新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目模板新增/编辑")
public class ProjectTemplateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID（编辑时必填）")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称长度不能超过128")
    private String templateName;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "描述")
    @Size(max = 512, message = "描述长度不能超过512")
    private String description;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
