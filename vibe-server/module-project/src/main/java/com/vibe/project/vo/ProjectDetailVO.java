package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 项目详情聚合 VO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目详情聚合")
public class ProjectDetailVO extends ProjectVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "阶段列表")
    private List<ProjectPhaseVO> phases;

    @Schema(description = "里程碑列表")
    private List<ProjectMilestoneVO> milestones;

    @Schema(description = "项目成员列表")
    private List<ProjectMemberVO> members;

    @Schema(description = "任务总数")
    private Integer taskTotal;

    @Schema(description = "已完成任务数")
    private Integer taskCompleted;

    @Schema(description = "进行中任务数")
    private Integer taskInProgress;

    @Schema(description = "待分配任务数")
    private Integer taskPending;

    @Schema(description = "风险数量")
    private Integer riskCount;

    @Schema(description = "问题数量")
    private Integer issueCount;
}
