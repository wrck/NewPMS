package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务转派 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "任务转派")
public class TaskTransferDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "新执行人ID（SELF 转派时填）")
    private Long newAssigneeId;

    @Schema(description = "新代理商公司ID（AGENT 转派时填）")
    private Long newAgentCompanyId;

    @Schema(description = "新代理商工程师ID（AGENT 转派时可选）")
    private Long newAgentEngineerId;

    @Schema(description = "转派原因")
    private String reason;
}
