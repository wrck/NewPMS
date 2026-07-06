package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 变更审批通过事件
 *
 * <p>触发时机：项目变更（范围/进度/资源/成本）申请审批通过后发布。
 * 下游消费者：通知引擎（推送变更通知给干系人）、ES 同步、项目计划调整。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "变更审批通过事件")
public class ChangeApprovedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "变更单ID")
    private Long changeId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "变更类型：SCOPE/SCHEDULE/RESOURCE/COST")
    private String changeType;

    @Schema(description = "变更影响级别：LOW/MEDIUM/HIGH")
    private String impactLevel;

    public ChangeApprovedEvent() {
        super(DomainEventConstant.EVENT_CHANGE_APPROVED, null);
    }

    public ChangeApprovedEvent(Long changeId, Long projectId, Long approverId,
                                String changeType, String impactLevel) {
        super(DomainEventConstant.EVENT_CHANGE_APPROVED, String.valueOf(changeId));
        this.changeId = changeId;
        this.projectId = projectId;
        this.approverId = approverId;
        this.changeType = changeType;
        this.impactLevel = impactLevel;
    }
}
