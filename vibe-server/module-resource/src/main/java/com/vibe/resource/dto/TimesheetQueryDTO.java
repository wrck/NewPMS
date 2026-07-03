package com.vibe.resource.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 工时分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工时分页查询")
public class TimesheetQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "状态 SUBMITTED/APPROVED/REJECTED")
    private String status;

    @Schema(description = "工作日期起")
    private LocalDate workDateStart;

    @Schema(description = "工作日期止")
    private LocalDate workDateEnd;
}
