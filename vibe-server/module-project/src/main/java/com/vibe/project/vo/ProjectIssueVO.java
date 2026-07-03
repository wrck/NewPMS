package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目问题视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目问题")
public class ProjectIssueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题ID")
    private Long id;

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

    @Schema(description = "责任人姓名（关联查询）")
    private String ownerName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "截止日期")
    private LocalDate dueDate;

    @Schema(description = "解决时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "是否超期")
    private Boolean overdue;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
