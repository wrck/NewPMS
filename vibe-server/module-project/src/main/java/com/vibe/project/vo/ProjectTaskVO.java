package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目任务视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目任务")
public class ProjectTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称（关联查询）")
    private String projectName;

    @Schema(description = "阶段ID")
    private Long phaseId;

    @Schema(description = "阶段名称（关联查询）")
    private String phaseName;

    @Schema(description = "父任务ID")
    private Long parentTaskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "执行模式")
    private String executeMode;

    @Schema(description = "执行人ID")
    private Long assigneeId;

    @Schema(description = "执行人姓名（关联查询）")
    private String assigneeName;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商公司名称（关联查询）")
    private String agentCompanyName;

    @Schema(description = "代理商工程师ID")
    private Long agentEngineerId;

    @Schema(description = "关联站点信息")
    private String siteInfo;

    @Schema(description = "关联设备ID列表")
    private String deviceIds;

    @Schema(description = "计划开始")
    private LocalDate plannedStart;

    @Schema(description = "计划结束")
    private LocalDate plannedEnd;

    @Schema(description = "实际开始")
    private LocalDate actualStart;

    @Schema(description = "实际结束")
    private LocalDate actualEnd;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "附件列表")
    private String attachments;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
