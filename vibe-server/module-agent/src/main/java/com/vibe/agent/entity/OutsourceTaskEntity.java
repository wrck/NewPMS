package com.vibe.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 转包任务实体（outsource_task）
 *
 * <p>对应 schema.sql 中的 outsource_task 表，是代理商管理模块的核心表。
 * 含 {@code version} 乐观锁字段（继承 {@link BaseEntity} 的 @Version），
 * 状态机由 {@link com.vibe.agent.enums.OutsourceTaskStatusEnum} 定义。</p>
 *
 * <p>状态流转：PENDING → ACCEPTED → IN_PROGRESS → SUBMITTED → CONFIRMED，
 * 异常分支 REJECTED / RETURNED / OVERDUE。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("outsource_task")
@Schema(description = "转包任务")
public class OutsourceTaskEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    @Schema(description = "项目ID")
    private Long projectId;

    /** 关联项目任务ID */
    @Schema(description = "关联项目任务ID")
    private Long taskId;

    /** 代理商公司ID */
    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    /** 代理商工程师ID（接单后填充） */
    @Schema(description = "代理商工程师ID")
    private Long agentEngineerId;

    /** 任务范围与要求 */
    @Schema(description = "任务范围与要求")
    private String taskScope;

    /** 截止日期 */
    @Schema(description = "截止日期")
    private LocalDate deadline;

    /**
     * 状态 PENDING/ACCEPTED/REJECTED/IN_PROGRESS/SUBMITTED/CONFIRMED/RETURNED/OVERDUE
     */
    @Schema(description = "任务状态")
    private String status;

    /** 提交次数 */
    @Schema(description = "提交次数")
    private Integer submitCount;

    /** 确认人ID */
    @Schema(description = "确认人ID")
    private Long confirmedBy;

    /** 确认时间 */
    @Schema(description = "确认时间")
    private LocalDateTime confirmedTime;

    /** 退回原因 */
    @Schema(description = "退回原因")
    private String rejectReason;

    // version 字段由 BaseEntity 提供（@Version），对应 schema.sql 中 version INT NOT NULL DEFAULT 1
}
