package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代理商消息 VO
 *
 * <p>用于代理商 H5 端消息通知列表展示，类似客户消息机制。</p>
 *
 * <p>消息类型（message_type）：</p>
 * <ul>
 *   <li>TASK_ASSIGNED    - 任务派发通知</li>
 *   <li>TASK_EXPIRING    - 任务即将到期提醒</li>
 *   <li>TASK_OVERDUE     - 任务超期警告</li>
 *   <li>DELIVERABLE_RETURNED - 交付物被退回通知</li>
 *   <li>DELIVERABLE_CONFIRMED - 交付物审核通过通知</li>
 *   <li>WORKLOAD_CONFIRMED - 工作量已确认通知</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "代理商消息")
public class AgentMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "关联业务ID（如 taskId/deliverableId）")
    private Long businessId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "是否已读 0-未读 1-已读")
    private Integer isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
