package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 项目状态变更事件
 *
 * <p>触发时机：项目状态流转（如 INIT→PLAN→EXEC→CLOSE→ARCHIVED）后发布。
 * 下游消费者：ES 同步（更新 vibe_project.status）、看板刷新、通知引擎。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目状态变更事件")
public class ProjectStatusChangedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "原状态")
    private String fromStatus;

    @Schema(description = "新状态")
    private String toStatus;

    @Schema(description = "当前阶段编码")
    private String currentPhase;

    public ProjectStatusChangedEvent() {
        super(DomainEventConstant.EVENT_PROJECT_STATUS_CHANGED, null);
    }

    public ProjectStatusChangedEvent(Long projectId, String fromStatus, String toStatus, String currentPhase) {
        super(DomainEventConstant.EVENT_PROJECT_STATUS_CHANGED, String.valueOf(projectId));
        this.projectId = projectId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.currentPhase = currentPhase;
    }
}
