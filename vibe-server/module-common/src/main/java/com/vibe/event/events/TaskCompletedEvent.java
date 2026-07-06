package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 任务完成事件
 *
 * <p>触发时机：项目任务标记完成后发布。
 * 下游消费者：项目进度统计、通知引擎、ES 同步。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "任务完成事件")
public class TaskCompletedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "执行人ID")
    private Long executorId;

    @Schema(description = "完成备注")
    private String remark;

    public TaskCompletedEvent() {
        super(DomainEventConstant.EVENT_TASK_COMPLETED, null);
    }

    public TaskCompletedEvent(Long taskId, Long projectId, Long executorId, String remark) {
        super(DomainEventConstant.EVENT_TASK_COMPLETED, String.valueOf(taskId));
        this.taskId = taskId;
        this.projectId = projectId;
        this.executorId = executorId;
        this.remark = remark;
    }
}
