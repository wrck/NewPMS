package com.vibe.agent.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 转包任务分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "转包任务查询")
public class OutsourceTaskQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字（项目名/任务名/代理商名）")
    private String keyword;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商工程师ID")
    private Long agentEngineerId;

    @Schema(description = "任务状态 PENDING/ACCEPTED/REJECTED/IN_PROGRESS/SUBMITTED/CONFIRMED/RETURNED/OVERDUE")
    private String status;

    @Schema(description = "仅查超期任务")
    private Boolean overdueOnly;
}
