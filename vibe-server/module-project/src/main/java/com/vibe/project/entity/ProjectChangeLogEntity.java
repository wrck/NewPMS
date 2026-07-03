package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 项目变更记录实体（project_change_log）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_change_log")
@Schema(description = "项目变更记录")
public class ProjectChangeLogEntity extends ProjectBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "变更类型 SCOPE/TIME/RESOURCE/OTHER")
    private String changeType;

    @Schema(description = "变更内容")
    private String changeContent;

    @Schema(description = "变更原因")
    private String reason;

    @Schema(description = "影响评估")
    private String impactAnalysis;

    @Schema(description = "状态 PENDING/APPROVED/REJECTED/EXECUTED")
    private String status;

    @Schema(description = "申请人ID")
    private Long applicantId;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;
}
