package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 工时统计多维查询 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工时统计多维查询")
public class TimesheetStatsQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "统计维度 ENGINEER/PROJECT/MONTHLY")
    private String dimension;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;
}
