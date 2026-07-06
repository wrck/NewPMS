package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 风险升级事件
 *
 * <p>触发时机：项目风险等级提升或被升级（如 HIGH→CRITICAL）后发布。
 * 下游消费者：通知引擎（推送升级通知给 PMO/总监）、ES 同步、风险看板刷新。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "风险升级事件")
public class RiskEscalatedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "风险ID")
    private Long riskId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "原风险等级")
    private String fromLevel;

    @Schema(description = "升级后风险等级")
    private String toLevel;

    @Schema(description = "风险描述")
    private String riskDescription;

    public RiskEscalatedEvent() {
        super(DomainEventConstant.EVENT_RISK_ESCALATED, null);
    }

    public RiskEscalatedEvent(Long riskId, Long projectId, String fromLevel, String toLevel, String riskDescription) {
        super(DomainEventConstant.EVENT_RISK_ESCALATED, String.valueOf(riskId));
        this.riskId = riskId;
        this.projectId = projectId;
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
        this.riskDescription = riskDescription;
    }
}
