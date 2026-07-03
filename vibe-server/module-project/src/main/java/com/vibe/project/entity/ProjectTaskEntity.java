package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 项目任务实体（project_task，含乐观锁）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_task")
@Schema(description = "项目任务")
public class ProjectTaskEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "所属阶段ID")
    private Long phaseId;

    @Schema(description = "父任务ID")
    private Long parentTaskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务类型 SURVEY/INSTALL/DEBUG/CUTOVER/ACCEPT/OTHER")
    private String taskType;

    @Schema(description = "状态 PENDING/ASSIGNED/IN_PROGRESS/COMPLETED/CONFIRMED")
    private String status;

    @Schema(description = "执行模式 SELF/AGENT")
    private String executeMode;

    @Schema(description = "执行人ID（自有工程师）")
    private Long assigneeId;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商工程师ID")
    private Long agentEngineerId;

    @Schema(description = "关联站点信息（JSON 字符串）")
    private String siteInfo;

    @Schema(description = "关联设备ID列表（JSON 字符串）")
    private String deviceIds;

    @Schema(description = "计划开始")
    private LocalDate plannedStart;

    @Schema(description = "计划结束")
    private LocalDate plannedEnd;

    @Schema(description = "实际开始")
    private LocalDate actualStart;

    @Schema(description = "实际结束")
    private LocalDate actualEnd;

    @Schema(description = "优先级 HIGH/MEDIUM/LOW")
    private String priority;

    @Schema(description = "任务描述与要求")
    private String description;

    @Schema(description = "附件列表（JSON 字符串）")
    private String attachments;
}
