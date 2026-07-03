package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目问题实体（project_issue）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_issue")
@Schema(description = "项目问题")
public class ProjectIssueEntity extends ProjectBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "关联任务ID")
    private Long taskId;

    @Schema(description = "问题描述")
    private String issueDesc;

    @Schema(description = "影响")
    private String impact;

    @Schema(description = "责任人ID")
    private Long ownerId;

    @Schema(description = "状态 OPEN/PROCESSING/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "截止日期")
    private LocalDate dueDate;

    @Schema(description = "解决时间")
    private LocalDateTime resolvedTime;
}
