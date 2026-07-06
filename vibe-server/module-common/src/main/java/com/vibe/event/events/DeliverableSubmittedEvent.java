package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 交付物提交事件
 *
 * <p>触发时机：项目交付物（文档/配置/截图等）提交后发布。
 * 下游消费者：通知引擎（推送给审核人）、ES 同步、交付物跟踪。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "交付物提交事件")
public class DeliverableSubmittedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "交付物ID")
    private Long deliverableId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "提交人ID")
    private Long submitterId;

    @Schema(description = "交付物名称")
    private String deliverableName;

    @Schema(description = "交付物类型")
    private String deliverableType;

    public DeliverableSubmittedEvent() {
        super(DomainEventConstant.EVENT_DELIVERABLE_SUBMITTED, null);
    }

    public DeliverableSubmittedEvent(Long deliverableId, Long projectId, Long submitterId,
                                      String deliverableName, String deliverableType) {
        super(DomainEventConstant.EVENT_DELIVERABLE_SUBMITTED, String.valueOf(deliverableId));
        this.deliverableId = deliverableId;
        this.projectId = projectId;
        this.submitterId = submitterId;
        this.deliverableName = deliverableName;
        this.deliverableType = deliverableType;
    }
}
