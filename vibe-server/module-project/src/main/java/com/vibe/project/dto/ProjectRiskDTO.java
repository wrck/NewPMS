package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目风险新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目风险新增/编辑")
public class ProjectRiskDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "风险ID（编辑时必填）")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "风险描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "风险描述不能为空")
    @Size(max = 512, message = "风险描述长度不能超过512")
    private String riskDesc;

    @Schema(description = "影响程度 HIGH/MEDIUM/LOW")
    private String impact;

    @Schema(description = "发生概率 HIGH/MEDIUM/LOW")
    private String probability;

    @Schema(description = "应对措施")
    @Size(max = 512, message = "应对措施长度不能超过512")
    private String measure;

    @Schema(description = "责任人ID")
    private Long ownerId;

    @Schema(description = "状态 OPEN/PROCESSING/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "截止日期")
    private LocalDate dueDate;
}
