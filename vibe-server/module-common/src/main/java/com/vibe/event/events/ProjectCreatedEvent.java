package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 项目立项事件
 *
 * <p>触发时机：项目立项成功后发布。下游消费者：ES 同步（写入 vibe_project 索引）、
 * 通知引擎（推送立项通知给 PM/客户）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目立项事件")
public class ProjectCreatedEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目经理ID")
    private Long pmId;

    @Schema(description = "客户名称")
    private String customerName;

    public ProjectCreatedEvent() {
        super(DomainEventConstant.EVENT_PROJECT_CREATED, null);
    }

    public ProjectCreatedEvent(Long projectId, String projectName, Long pmId, String customerName) {
        super(DomainEventConstant.EVENT_PROJECT_CREATED, String.valueOf(projectId));
        this.projectId = projectId;
        this.projectName = projectName;
        this.pmId = pmId;
        this.customerName = customerName;
    }
}
