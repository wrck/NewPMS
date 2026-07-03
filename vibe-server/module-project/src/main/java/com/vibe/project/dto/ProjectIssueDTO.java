package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目问题新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目问题新增/编辑")
public class ProjectIssueDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题ID（编辑时必填）")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "关联任务ID")
    private Long taskId;

    @Schema(description = "问题描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题描述不能为空")
    @Size(max = 512, message = "问题描述长度不能超过512")
    private String issueDesc;

    @Schema(description = "影响")
    @Size(max = 255, message = "影响长度不能超过255")
    private String impact;

    @Schema(description = "责任人ID")
    private Long ownerId;

    @Schema(description = "状态 OPEN/PROCESSING/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "截止日期")
    private LocalDate dueDate;
}
