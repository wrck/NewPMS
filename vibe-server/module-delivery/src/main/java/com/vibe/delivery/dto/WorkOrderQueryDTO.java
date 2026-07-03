package com.vibe.delivery.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 工单分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工单分页查询")
public class WorkOrderQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务名称/项目名称（模糊）")
    private String keyword;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "项目任务ID")
    private Long taskId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "工单状态")
    private String status;

    @Schema(description = "签到开始日期")
    private LocalDate checkinStart;

    @Schema(description = "签到结束日期")
    private LocalDate checkinEnd;
}
