package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 代理商评分事件
 *
 * <p>触发时机：项目结束后对代理商进行评分（质量/进度/配合度/成本）后发布。
 * 下游消费者：代理商画像更新、ES 同步、通知引擎（推送评分给代理商）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代理商评分事件")
public class AgentScoredEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评分单ID")
    private Long scoreId;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "综合得分（0-100）")
    private Double totalScore;

    @Schema(description = "质量得分")
    private Double qualityScore;

    @Schema(description = "进度得分")
    private Double scheduleScore;

    public AgentScoredEvent() {
        super(DomainEventConstant.EVENT_AGENT_SCORED, null);
    }

    public AgentScoredEvent(Long scoreId, Long agentCompanyId, Long projectId,
                             Double totalScore, Double qualityScore, Double scheduleScore) {
        super(DomainEventConstant.EVENT_AGENT_SCORED, String.valueOf(scoreId));
        this.scoreId = scoreId;
        this.agentCompanyId = agentCompanyId;
        this.projectId = projectId;
        this.totalScore = totalScore;
        this.qualityScore = qualityScore;
        this.scheduleScore = scheduleScore;
    }
}
