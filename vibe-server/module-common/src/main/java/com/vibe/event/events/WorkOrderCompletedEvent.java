package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 工单完成事件
 *
 * <p>触发时机：工单被 PM 确认完成后发布。
 * 下游消费者：ES 同步（更新 vibe_work_order.status=CONFIRMED）、项目任务进度推进、通知引擎。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工单完成事件")
public class WorkOrderCompletedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "项目任务ID")
    private Long taskId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "实际结束时间（签退时间）")
    private LocalDateTime actualEnd;

    public WorkOrderCompletedEvent() {
        super(DomainEventConstant.EVENT_WORK_ORDER_COMPLETED, null);
    }

    public WorkOrderCompletedEvent(Long workOrderId, Long taskId, Long projectId, Long engineerId,
                                    String engineerName, LocalDateTime actualEnd) {
        super(DomainEventConstant.EVENT_WORK_ORDER_COMPLETED, String.valueOf(workOrderId));
        this.workOrderId = workOrderId;
        this.taskId = taskId;
        this.projectId = projectId;
        this.engineerId = engineerId;
        this.engineerName = engineerName;
        this.actualEnd = actualEnd;
    }
}
