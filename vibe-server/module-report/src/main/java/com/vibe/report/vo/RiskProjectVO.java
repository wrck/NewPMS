package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 风险项目 VO
 *
 * <p>用于驾驶舱与首页风险项目列表展示，标识项目当前面临的风险类型与描述。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "风险项目")
public class RiskProjectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "风险类型 PROGRESS_DELAY/OVERDUE_TASK/UNRESOLVED_ISSUE/PROJECT_OVERDUE")
    private String riskType;

    @Schema(description = "风险类型名称")
    private String riskTypeName;

    @Schema(description = "风险描述")
    private String description;

    @Schema(description = "进度百分比")
    private Integer progressPct;

    @Schema(description = "项目经理姓名")
    private String pmName;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEnd;

    @Schema(description = "项目状态")
    private String status;
}
