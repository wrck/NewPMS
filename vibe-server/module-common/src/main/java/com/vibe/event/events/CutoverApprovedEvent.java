package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 割接审批通过事件
 *
 * <p>触发时机：网络割接方案审批通过后发布。
 * 下游消费者：通知引擎（推送割接通知给客户/运维）、ES 同步、割接计划执行。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "割接审批通过事件")
public class CutoverApprovedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "割接单ID")
    private Long cutoverId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "计划割接时间")
    private String plannedCutoverTime;

    public CutoverApprovedEvent() {
        super(DomainEventConstant.EVENT_CUTOVER_APPROVED, null);
    }

    public CutoverApprovedEvent(Long cutoverId, Long projectId, Long approverId, String plannedCutoverTime) {
        super(DomainEventConstant.EVENT_CUTOVER_APPROVED, String.valueOf(cutoverId));
        this.cutoverId = cutoverId;
        this.projectId = projectId;
        this.approverId = approverId;
        this.plannedCutoverTime = plannedCutoverTime;
    }
}
