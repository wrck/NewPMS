package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 任务派发事件
 *
 * <p>触发时机：项目任务派发给工程师/代理商后发布。
 * 下游消费者：通知引擎（推送任务分配通知）、工单生成、ES 同步。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "任务派发事件")
public class TaskAssignedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "被指派人ID")
    private Long assigneeId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "执行模式：SELF/OUTSOURCE")
    private String executeMode;

    public TaskAssignedEvent() {
        super(DomainEventConstant.EVENT_TASK_ASSIGNED, null);
    }

    public TaskAssignedEvent(Long taskId, Long projectId, Long assigneeId, String taskName, String executeMode) {
        super(DomainEventConstant.EVENT_TASK_ASSIGNED, String.valueOf(taskId));
        this.taskId = taskId;
        this.projectId = projectId;
        this.assigneeId = assigneeId;
        this.taskName = taskName;
        this.executeMode = executeMode;
    }
}
