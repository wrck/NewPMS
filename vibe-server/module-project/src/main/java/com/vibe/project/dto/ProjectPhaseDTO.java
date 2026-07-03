package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目阶段新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目阶段新增/编辑")
public class ProjectPhaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "阶段ID（编辑时必填）")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "阶段编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "阶段编码不能为空")
    @Size(max = 32, message = "阶段编码长度不能超过32")
    private String phaseCode;

    @Schema(description = "阶段名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "阶段名称不能为空")
    @Size(max = 64, message = "阶段名称长度不能超过64")
    private String phaseName;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 NOT_STARTED/IN_PROGRESS/COMPLETED")
    private String status;

    @Schema(description = "计划开始")
    private LocalDate plannedStart;

    @Schema(description = "计划结束")
    private LocalDate plannedEnd;

    @Schema(description = "实际开始")
    private LocalDate actualStart;

    @Schema(description = "实际结束")
    private LocalDate actualEnd;

    @Schema(description = "交付物清单（JSON 字符串）")
    private String deliverables;
}
