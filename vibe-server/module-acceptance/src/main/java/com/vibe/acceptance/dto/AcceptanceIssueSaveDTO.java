package com.vibe.acceptance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 遗留问题创建/更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "遗留问题创建/更新")
public class AcceptanceIssueSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（更新时必填）")
    private Long id;

    @NotNull(message = "验收任务ID不能为空")
    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "关联项目ID")
    private Long projectId;

    @NotBlank(message = "问题名称不能为空")
    @Schema(description = "遗留问题名称")
    private String name;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "严重等级 LOW/MEDIUM/HIGH/CRITICAL")
    private String severity;

    @Schema(description = "整改责任人ID")
    private Long assigneeId;

    @Schema(description = "整改截止日期")
    private LocalDate dueDate;

    @Schema(description = "备注")
    private String remark;
}
