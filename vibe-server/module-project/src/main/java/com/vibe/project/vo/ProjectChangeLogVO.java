package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目变更记录视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目变更记录")
public class ProjectChangeLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "变更ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "变更类型")
    private String changeType;

    @Schema(description = "变更内容")
    private String changeContent;

    @Schema(description = "变更原因")
    private String reason;

    @Schema(description = "影响评估")
    private String impactAnalysis;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "申请人ID")
    private Long applicantId;

    @Schema(description = "申请人姓名（关联查询）")
    private String applicantName;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批人姓名（关联查询）")
    private String approverName;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
