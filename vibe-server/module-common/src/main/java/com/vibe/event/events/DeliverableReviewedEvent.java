package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 交付物审核事件
 *
 * <p>触发时机：交付物被审核（通过/退回）后发布。
 * 下游消费者：通知引擎（推送审核结果给提交人）、ES 同步、项目里程碑更新。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "交付物审核事件")
public class DeliverableReviewedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "交付物ID")
    private Long deliverableId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "审核人ID")
    private Long reviewerId;

    @Schema(description = "审核结果：APPROVED/REJECTED")
    private String reviewResult;

    @Schema(description = "审核意见")
    private String reviewComment;

    public DeliverableReviewedEvent() {
        super(DomainEventConstant.EVENT_DELIVERABLE_REVIEWED, null);
    }

    public DeliverableReviewedEvent(Long deliverableId, Long projectId, Long reviewerId,
                                     String reviewResult, String reviewComment) {
        super(DomainEventConstant.EVENT_DELIVERABLE_REVIEWED, String.valueOf(deliverableId));
        this.deliverableId = deliverableId;
        this.projectId = projectId;
        this.reviewerId = reviewerId;
        this.reviewResult = reviewResult;
        this.reviewComment = reviewComment;
    }
}
