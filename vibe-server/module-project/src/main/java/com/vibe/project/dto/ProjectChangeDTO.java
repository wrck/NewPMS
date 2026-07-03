package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目变更申请 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目变更申请")
public class ProjectChangeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "变更类型 SCOPE/TIME/RESOURCE/OTHER")
    private String changeType;

    @Schema(description = "变更内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "变更内容不能为空")
    private String changeContent;

    @Schema(description = "变更原因")
    @Size(max = 512, message = "变更原因长度不能超过512")
    private String reason;

    @Schema(description = "影响评估")
    private String impactAnalysis;
}
