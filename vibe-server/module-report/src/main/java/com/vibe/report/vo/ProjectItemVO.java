package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目列表项 VO
 *
 * <p>PM 首页"我的项目"、驾驶舱风险项目列表共用。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "项目列表项")
public class ProjectItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "当前阶段")
    private String currentPhase;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "进度百分比")
    private Integer progressPct;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEnd;

    @Schema(description = "项目经理姓名")
    private String pmName;

    @Schema(description = "客户名称")
    private String customerName;
}
