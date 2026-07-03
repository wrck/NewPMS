package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务派发 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "任务派发")
public class TaskDispatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "执行模式 SELF/AGENT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "执行模式不能为空")
    private String executeMode;

    @Schema(description = "执行人ID（SELF 模式必填）")
    private Long assigneeId;

    @Schema(description = "代理商公司ID（AGENT 模式必填）")
    private Long agentCompanyId;

    @Schema(description = "代理商工程师ID（AGENT 模式可选）")
    private Long agentEngineerId;

    @Schema(description = "任务范围与要求（AGENT 模式下作为转包任务范围）")
    private String taskScope;

    @Schema(description = "截止日期（yyyy-MM-dd，AGENT 模式下作为转包任务截止日期）")
    private String deadline;
}
