package com.vibe.system.notice.listener;

import com.vibe.event.DomainEventConstant;
import com.vibe.system.notification.NotificationConstant;
import com.vibe.system.notification.NotificationEvent;
import com.vibe.system.notification.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 领域事件 → 通知引擎 触发监听器
 *
 * <p>监听 RabbitMQ 队列 {@code vibe.notice.trigger.queue}，消费领域事件总线上的全部事件，
 * 按 eventType 路由到对应通知模板（使用现有 9 个 MVP 模板），通过 {@link NotificationProducer}
 * 投递到通知引擎 {@code vibe.notification.exchange}，由 {@code NotificationConsumer} 异步渲染与发送。</p>
 *
 * <p>事件 → 通知模板映射（spec 定义）：</p>
 * <ul>
 *   <li>{@code TASK_ASSIGNED} → TASK_ASSIGNED 模板</li>
 *   <li>{@code WORK_ORDER_COMPLETED} → TASK_REMINDER 模板（即将到期提醒）</li>
 *   <li>{@code WORK_ORDER_COMPLETED} + 检测到超期 → TASK_OVERDUE 模板</li>
 *   <li>{@code DELIVERABLE_SUBMITTED} → DELIVERABLE_REVIEW 模板</li>
 *   <li>{@code DELIVERABLE_REVIEWED} (REJECTED/RETURNED) → DELIVERABLE_RETURNED 模板</li>
 *   <li>{@code DELIVERABLE_REVIEWED} (APPROVED/CONFIRMED) → DELIVERABLE_CONFIRMED 模板</li>
 *   <li>{@code DEVICE_STATUS_CHANGED} (SHIPPED→RECEIVED) → DEVICE_ARRIVED 模板</li>
 *   <li>{@code DEVICE_STATUS_CHANGED} (异常状态) → DEVICE_ABNORMAL 模板</li>
 *   <li>{@code INVENTORY_WARNING} → DEVICE_ABNORMAL 模板（库存维度）</li>
 *   <li>{@code RISK_ESCALATED} → RISK_WARNING 模板</li>
 * </ul>
 *
 * <p><b>重复触发说明</b>：</p>
 * <p>部分业务模块（如 {@code ProjectTaskServiceImpl.sendTaskAssignedNotification}）已在业务流程中
 * 直接调用 {@code NotificationProducer.send} 发送通知事件，本监听器是「事件总线驱动」的补充触发器，
 * 重复通知由 {@link com.vibe.system.notification.router.NotificationChannelRouter} 的频率控制
 * （同一 recipientId+eventType 5 分钟内仅发一次）兜底去重，不会造成用户感知的重复触达。</p>
 *
 * <p>容错策略：</p>
 * <ul>
 *   <li>反序列化或路由失败仅记日志，消息正常 ACK，避免无限重试堆积</li>
 *   <li>通知引擎不可达时由 {@code NotificationProducer} 内部吞掉异常，不影响事件消费</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeEventListener {

    /** 设备状态异常集合：流转到这些状态时触发 DEVICE_ABNORMAL 模板 */
    private static final List<String> ABNORMAL_DEVICE_STATUSES =
            List.of("DAMAGED", "LOST", "REPAIR", "FAULTY", "SCRAPPED");

    private final NotificationProducer notificationProducer;

    /**
     * 消费领域事件，按 eventType 路由到对应通知模板。
     *
     * <p>使用具体事件类接收消息体，由 {@code Jackson2JsonMessageConverter} 反序列化为 POJO，
     * 依赖事件类的默认构造函数（无参）+ setter。</p>
     *
     * @param event 领域事件（JSON 反序列化为具体子类，仅包含子类字段）
     */
    @RabbitListener(queues = DomainEventConstant.NOTICE_TRIGGER_QUEUE)
    public void onDomainEvent(@Payload Map<String, Object> event) {
        if (event == null) {
            log.warn("[NoticeTrigger] 收到空领域事件，ACK 丢弃");
            return;
        }
        String eventType = asString(event.get("eventType"));
        String eventId = asString(event.get("eventId"));
        String businessKey = asString(event.get("businessKey"));
        log.info("[NoticeTrigger] 收到领域事件: eventId={}, eventType={}, businessKey={}",
                eventId, eventType, businessKey);
        if (eventType == null || eventType.isBlank()) {
            log.warn("[NoticeTrigger] 领域事件 eventType 为空，ACK 丢弃: event={}", event);
            return;
        }
        try {
            dispatch(eventType, event);
        } catch (Exception e) {
            // 路由失败仅记日志，不抛异常（ACK 丢弃），保证业务不受影响
            log.error("[NoticeTrigger] 通知路由失败（ACK 丢弃）: eventId={}, eventType={}, error={}",
                    eventId, eventType, e.getMessage(), e);
        }
    }

    /**
     * 按事件类型分发到对应的通知模板。
     */
    private void dispatch(String eventType, Map<String, Object> event) {
        switch (eventType) {
            case DomainEventConstant.EVENT_TASK_ASSIGNED ->
                    handleTaskAssigned(event);
            case DomainEventConstant.EVENT_WORK_ORDER_COMPLETED ->
                    handleWorkOrderCompleted(event);
            case DomainEventConstant.EVENT_DELIVERABLE_SUBMITTED ->
                    handleDeliverableSubmitted(event);
            case DomainEventConstant.EVENT_DELIVERABLE_REVIEWED ->
                    handleDeliverableReviewed(event);
            case DomainEventConstant.EVENT_DEVICE_STATUS_CHANGED ->
                    handleDeviceStatusChanged(event);
            case DomainEventConstant.EVENT_INVENTORY_WARNING ->
                    handleInventoryWarning(event);
            case DomainEventConstant.EVENT_RISK_ESCALATED ->
                    handleRiskEscalated(event);
            default ->
                    log.debug("[NoticeTrigger] 事件类型 {} 无通知模板映射，跳过", eventType);
        }
    }

    /**
     * TaskAssignedEvent → TASK_ASSIGNED 模板。
     */
    private void handleTaskAssigned(Map<String, Object> event) {
        Long assigneeId = asLong(event.get("assigneeId"));
        String executeMode = asString(event.get("executeMode"));
        String recipientType = "AGENT".equals(executeMode)
                ? NotificationConstant.RECIPIENT_AGENT
                : NotificationConstant.RECIPIENT_INTERNAL;
        Map<String, String> variables = new HashMap<>(4);
        variables.put("taskName", asString(event.get("taskName")));
        variables.put("projectName", "");
        variables.put("plannedStart", "");
        variables.put("plannedEnd", "");
        List<Long> recipientIds = assigneeId == null
                ? Collections.emptyList()
                : Collections.singletonList(assigneeId);
        NotificationEvent notify = NotificationEvent.of(
                NotificationConstant.EVENT_TASK_ASSIGNED,
                recipientType, recipientIds, variables,
                asLong(event.get("taskId")), NotificationConstant.BIZ_PROJECT_TASK);
        notificationProducer.send(notify);
    }

    /**
     * WorkOrderCompletedEvent → TASK_REMINDER 或 TASK_OVERDUE 模板。
     *
     * <p>简化逻辑：本事件携带 actualEnd（实际结束时间），但由于不携带 plannedEnd（计划结束时间），
     * 无法直接判断是否超期。这里默认路由到 TASK_REMINDER 模板（即将到期提醒）。
     * 若后续事件携带 plannedEnd 字段且 actualEnd > plannedEnd，则路由到 TASK_OVERDUE。</p>
     */
    private void handleWorkOrderCompleted(Map<String, Object> event) {
        Long engineerId = asLong(event.get("engineerId"));
        Long workOrderId = asLong(event.get("workOrderId"));
        String templateCode = NotificationConstant.EVENT_TASK_REMINDER;
        // 若 actualEnd 晚于 plannedEnd（超期），路由到 TASK_OVERDUE
        Object actualEndObj = event.get("actualEnd");
        // 简化：actualEnd 存在视为已完成，无法判断超期；保留分支便于扩展
        if (actualEndObj != null) {
            templateCode = NotificationConstant.EVENT_TASK_REMINDER;
        }
        Map<String, String> variables = new HashMap<>(4);
        variables.put("taskName", "");
        variables.put("projectName", "");
        variables.put("plannedEnd", "");
        List<Long> recipientIds = engineerId == null
                ? Collections.emptyList()
                : Collections.singletonList(engineerId);
        NotificationEvent notify = NotificationEvent.of(
                templateCode, NotificationConstant.RECIPIENT_INTERNAL,
                recipientIds, variables, workOrderId, NotificationConstant.BIZ_PROJECT_TASK);
        notificationProducer.send(notify);
    }

    /**
     * DeliverableSubmittedEvent → DELIVERABLE_REVIEW 模板（通知 PM 审核）。
     */
    private void handleDeliverableSubmitted(Map<String, Object> event) {
        Long deliverableId = asLong(event.get("deliverableId"));
        Map<String, String> variables = new HashMap<>(4);
        variables.put("taskName", asString(event.get("deliverableName")));
        variables.put("projectName", "");
        variables.put("submitCount", "1");
        NotificationEvent notify = NotificationEvent.of(
                NotificationConstant.EVENT_DELIVERABLE_REVIEW,
                NotificationConstant.RECIPIENT_INTERNAL,
                Collections.emptyList(), variables,
                deliverableId, NotificationConstant.BIZ_OUTSOURCE_TASK);
        notificationProducer.send(notify);
    }

    /**
     * DeliverableReviewedEvent → DELIVERABLE_CONFIRMED 或 DELIVERABLE_RETURNED 模板。
     *
     * <p>reviewResult 字段约定：APPROVED/CONFIRMED → CONFIRMED 模板；
     * REJECTED/RETURNED → RETURNED 模板。</p>
     */
    private void handleDeliverableReviewed(Map<String, Object> event) {
        String reviewResult = asString(event.get("reviewResult"));
        String templateCode;
        if ("APPROVED".equalsIgnoreCase(reviewResult)
                || "CONFIRMED".equalsIgnoreCase(reviewResult)) {
            templateCode = NotificationConstant.EVENT_DELIVERABLE_CONFIRMED;
        } else {
            templateCode = NotificationConstant.EVENT_DELIVERABLE_RETURNED;
        }
        Map<String, String> variables = new HashMap<>(4);
        variables.put("taskName", asString(event.get("deliverableName")));
        variables.put("projectName", "");
        variables.put("rejectReason", asString(event.get("reviewComment")));
        // 交付物审核结果通知接收人：发布方未携带，由通知引擎飞书群触达
        NotificationEvent notify = NotificationEvent.of(
                templateCode, NotificationConstant.RECIPIENT_AGENT,
                Collections.emptyList(), variables,
                asLong(event.get("deliverableId")), NotificationConstant.BIZ_OUTSOURCE_TASK);
        notificationProducer.send(notify);
    }

    /**
     * DeviceStatusChangedEvent → DEVICE_ARRIVED 或 DEVICE_ABNORMAL 模板。
     *
     * <p>SHIPPED → RECEIVED 触发 DEVICE_ARRIVED；流转到异常状态（DAMAGED/LOST/REPAIR/FAULTY/SCRAPPED）
     * 触发 DEVICE_ABNORMAL。</p>
     */
    private void handleDeviceStatusChanged(Map<String, Object> event) {
        String fromStatus = asString(event.get("fromStatus"));
        String toStatus = asString(event.get("toStatus"));
        Long deviceId = asLong(event.get("deviceId"));
        String sn = asString(event.get("sn"));
        if ("SHIPPED".equals(fromStatus) && "RECEIVED".equals(toStatus)) {
            Map<String, String> variables = new HashMap<>(4);
            variables.put("modelName", "");
            variables.put("projectName", "");
            variables.put("quantity", "1");
            NotificationEvent notify = NotificationEvent.of(
                    NotificationConstant.EVENT_DEVICE_ARRIVED,
                    NotificationConstant.RECIPIENT_INTERNAL,
                    Collections.emptyList(), variables,
                    deviceId, NotificationConstant.BIZ_DEVICE);
            notificationProducer.send(notify);
        } else if (toStatus != null && ABNORMAL_DEVICE_STATUSES.contains(toStatus.toUpperCase())) {
            Map<String, String> variables = new HashMap<>(4);
            variables.put("serialNumber", sn == null ? "" : sn);
            variables.put("modelName", "");
            variables.put("projectName", "");
            variables.put("abnormalDesc", toStatus);
            NotificationEvent notify = NotificationEvent.of(
                    NotificationConstant.EVENT_DEVICE_ABNORMAL,
                    NotificationConstant.RECIPIENT_INTERNAL,
                    Collections.emptyList(), variables,
                    deviceId, NotificationConstant.BIZ_DEVICE);
            notificationProducer.send(notify);
        }
    }

    /**
     * InventoryWarningEvent → DEVICE_ABNORMAL 模板（库存维度）。
     */
    private void handleInventoryWarning(Map<String, Object> event) {
        Long modelId = asLong(event.get("modelId"));
        Map<String, String> variables = new HashMap<>(8);
        variables.put("serialNumber", "");
        variables.put("modelName", asString(event.get("modelName")));
        variables.put("projectName", "");
        variables.put("abnormalDesc", "库存预警: 仓库=" + asString(event.get("warehouseName"))
                + ", 当前=" + asString(event.get("currentStock"))
                + ", 安全库存=" + asString(event.get("safetyStock"))
                + ", 级别=" + asString(event.get("level")));
        NotificationEvent notify = NotificationEvent.of(
                NotificationConstant.EVENT_DEVICE_ABNORMAL,
                NotificationConstant.RECIPIENT_INTERNAL,
                Collections.emptyList(), variables,
                modelId, NotificationConstant.BIZ_DEVICE);
        notificationProducer.send(notify);
    }

    /**
     * RiskEscalatedEvent → RISK_WARNING 模板。
     */
    private void handleRiskEscalated(Map<String, Object> event) {
        Long riskId = asLong(event.get("riskId"));
        Map<String, String> variables = new HashMap<>(4);
        variables.put("projectName", "");
        variables.put("riskDesc", asString(event.get("riskDescription")));
        variables.put("impact", "");
        variables.put("probability", "");
        NotificationEvent notify = NotificationEvent.of(
                NotificationConstant.EVENT_RISK_WARNING,
                NotificationConstant.RECIPIENT_INTERNAL,
                Collections.emptyList(), variables,
                riskId, NotificationConstant.BIZ_PROJECT_RISK);
        notificationProducer.send(notify);
    }

    /* ============ 类型转换工具方法 ============ */

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Long asLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
