package com.vibe.acceptance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 验收遗留问题实体（acceptance_issue 表，含 @Version 乐观锁）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acceptance_issue")
@Schema(description = "验收遗留问题")
public class AcceptanceIssueEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "遗留问题名称")
    private String name;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "严重等级 LOW/MEDIUM/HIGH/CRITICAL")
    private String severity;

    @Schema(description = "整改责任人ID")
    private Long assigneeId;

    @Schema(description = "整改截止日期")
    private LocalDate dueDate;

    @Schema(description = "整改完成时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "状态 OPEN/IN_PROGRESS/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "闭环确认人ID")
    private Long closeUserId;

    @Schema(description = "闭环确认时间")
    private LocalDateTime closeTime;

    @Schema(description = "备注")
    private String remark;
}
