package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单创建 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工单创建")
public class WorkOrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目任务ID不能为空")
    private Long taskId;

    @Schema(description = "执行工程师ID（不填则取 project_task.assignee_id）")
    private Long engineerId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "标准施工步骤名称列表（不传则按默认模板初始化）")
    private java.util.List<String> steps;
}
