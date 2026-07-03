package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目模板视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目模板")
public class ProjectTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
