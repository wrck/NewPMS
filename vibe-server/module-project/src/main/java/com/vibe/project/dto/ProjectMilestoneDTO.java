package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目里程碑新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目里程碑新增/编辑")
public class ProjectMilestoneDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "里程碑ID（编辑时必填）")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "里程碑名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "里程碑名称不能为空")
    @Size(max = 128, message = "里程碑名称长度不能超过128")
    private String milestoneName;

    @Schema(description = "预计日期")
    private LocalDate plannedDate;

    @Schema(description = "实际日期")
    private LocalDate actualDate;

    @Schema(description = "交付物清单（JSON 字符串）")
    private String deliverables;

    @Schema(description = "状态 PENDING/REACHED/DELAYED")
    private String status;
}
