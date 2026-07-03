package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工时统计项（人天/工时聚合）
 *
 * @author vibe
 */
@Data
@Schema(description = "工时统计项")
public class TimesheetStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID（按工程师维度时返回）")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "项目ID（按项目维度时返回）")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "统计键（按月度维度时为 yyyy-MM）")
    private String statKey;

    @Schema(description = "总工时（小时）")
    private BigDecimal totalHours;

    @Schema(description = "总加班时长（小时）")
    private BigDecimal totalOvertimeHours;

    @Schema(description = "总出差天数")
    private Integer totalTravelDays;

    @Schema(description = "总人天（按 8 小时/天折算）")
    private BigDecimal totalManDays;

    @Schema(description = "记录数")
    private Integer recordCount;
}
