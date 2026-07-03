package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 转包任务列表项 VO
 *
 * <p>代理商首页待接单/进行中/待审核任务列表展示。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "转包任务列表项")
public class OutsourceTaskItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "转包任务ID")
    private Long outsourceTaskId;

    @Schema(description = "项目任务ID")
    private Long projectTaskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商公司名称")
    private String agentCompanyName;

    @Schema(description = "代理商工程师ID")
    private Long agentEngineerId;

    @Schema(description = "代理商工程师姓名")
    private String agentEngineerName;

    @Schema(description = "转包任务状态")
    private String status;

    @Schema(description = "任务范围与要求")
    private String taskScope;

    @Schema(description = "截止日期")
    private LocalDate deadline;

    @Schema(description = "提交次数")
    private Integer submitCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "是否超期（1-是 0-否）")
    private Integer overdue;
}
