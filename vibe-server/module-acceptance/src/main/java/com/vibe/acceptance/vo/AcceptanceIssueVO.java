package com.vibe.acceptance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 验收遗留问题 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "验收遗留问题")
public class AcceptanceIssueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
