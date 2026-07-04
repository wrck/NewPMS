package com.vibe.acceptance.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 遗留问题分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "遗留问题查询")
public class AcceptanceIssueQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "严重等级")
    private String severity;

    @Schema(description = "整改责任人ID")
    private Long assigneeId;
}
