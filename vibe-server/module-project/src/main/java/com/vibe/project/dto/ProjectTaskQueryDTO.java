package com.vibe.project.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 项目任务分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目任务分页查询")
public class ProjectTaskQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "阶段ID")
    private Long phaseId;

    @Schema(description = "父任务ID（不传则查顶层）")
    private Long parentTaskId;

    @Schema(description = "任务名称（模糊）")
    private String keyword;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "执行人ID")
    private Long assigneeId;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商工程师ID")
    private Long agentEngineerId;

    @Schema(description = "执行模式")
    private String executeMode;

    @Schema(description = "任务类型")
    private String taskType;
}
