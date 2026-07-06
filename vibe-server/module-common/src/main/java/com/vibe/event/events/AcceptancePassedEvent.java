package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 验收通过事件
 *
 * <p>触发时机：项目验收阶段通过后发布。
 * 下游消费者：通知引擎（推送验收通过通知）、ES 同步、项目状态推进。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "验收通过事件")
public class AcceptancePassedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "验收单ID")
    private Long acceptanceId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "验收结论：PASSED/CONDITIONAL")
    private String conclusion;

    @Schema(description = "验收人")
    private String acceptor;

    public AcceptancePassedEvent() {
        super(DomainEventConstant.EVENT_ACCEPTANCE_PASSED, null);
    }

    public AcceptancePassedEvent(Long acceptanceId, Long projectId, Long customerId,
                                  String conclusion, String acceptor) {
        super(DomainEventConstant.EVENT_ACCEPTANCE_PASSED, String.valueOf(acceptanceId));
        this.acceptanceId = acceptanceId;
        this.projectId = projectId;
        this.customerId = customerId;
        this.conclusion = conclusion;
        this.acceptor = acceptor;
    }
}
