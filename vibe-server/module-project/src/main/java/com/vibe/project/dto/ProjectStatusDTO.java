package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目状态流转 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目状态流转")
public class ProjectStatusDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目ID不能为空")
    private Long id;

    @Schema(description = "目标状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    @Schema(description = "当前乐观锁版本号（可选，未传则取最新）")
    private Integer version;

    @Schema(description = "流转备注/原因")
    private String remark;
}
