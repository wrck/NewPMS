package com.vibe.system.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 通知事件 DTO
 *
 * <p>业务模块在关键节点构造本对象，通过 {@code NotificationProducer.send()} 投递到 RabbitMQ，
 * 由消费侧异步完成模板渲染、渠道路由与多渠道发送。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code eventType} - 事件类型，对应 routing key 后缀，如 TASK_ASSIGNED</li>
 *   <li>{@code templateCode} - 通知模板编码（通常与 eventType 一致），用于加载标题/内容模板</li>
 *   <li>{@code recipientIds} - 接收人用户 ID 列表</li>
 *   <li>{@code recipientType} - 接收人类型 INTERNAL/AGENT/CUSTOMER，决定渠道选择</li>
 *   <li>{@code variables} - 模板变量 Map，渲染时将 ${key} 替换为 value</li>
 *   <li>{@code businessId} - 关联业务对象 ID（便于追溯）</li>
 *   <li>{@code businessType} - 关联业务类型，如 PROJECT_TASK/DEVICE</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "通知事件")
public class NotificationEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "事件类型（routing key 后缀）", example = "TASK_ASSIGNED")
    private String eventType;

    @Schema(description = "通知模板编码", example = "TASK_ASSIGNED")
    private String templateCode;

    @Schema(description = "接收人用户 ID 列表")
    private List<Long> recipientIds;

    @Schema(description = "接收人类型 INTERNAL/AGENT/CUSTOMER")
    private String recipientType;

    @Schema(description = "模板变量 Map（key 为变量名，value 为替换值）")
    private Map<String, String> variables;

    @Schema(description = "关联业务对象 ID")
    private Long businessId;

    @Schema(description = "关联业务类型")
    private String businessType;

    /**
     * 构造一个指定事件类型与模板编码的事件（eventType 与 templateCode 一致）。
     */
    public static NotificationEvent of(String eventType, String recipientType,
                                       List<Long> recipientIds, Map<String, String> variables,
                                       Long businessId, String businessType) {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(eventType);
        event.setTemplateCode(eventType);
        event.setRecipientType(recipientType);
        event.setRecipientIds(recipientIds);
        event.setVariables(variables);
        event.setBusinessId(businessId);
        event.setBusinessType(businessType);
        return event;
    }
}
