package com.vibe.system.notification.producer;

import com.vibe.system.notification.NotificationConstant;
import com.vibe.system.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知事件生产者
 *
 * <p>业务模块在关键节点（任务派发、交付物提交/审核、设备到货/异常、风险预警等）
 * 调用 {@link #send(NotificationEvent)} 将事件投递到 RabbitMQ。</p>
 *
 * <p>投递规则：</p>
 * <ul>
 *   <li>Exchange：{@code vibe.notification.exchange}（Topic）</li>
 *   <li>Routing Key：{@code vibe.notification.{eventType}}，如 vibe.notification.TASK_ASSIGNED</li>
 *   <li>消息体：{@link NotificationEvent} JSON 序列化</li>
 * </ul>
 *
 * <p>容错策略：投递失败仅记日志、不抛异常，避免阻塞业务流程。
 * RabbitMQ 在 MVP 阶段可暂不启动，业务仍可正常运行。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final RabbitTemplate notificationRabbitTemplate;

    /**
     * 投递通知事件到 RabbitMQ。
     *
     * <p>异步、非阻塞、容错：投递失败时仅记日志，不影响业务事务。</p>
     *
     * @param event 通知事件
     */
    public void send(NotificationEvent event) {
        if (event == null || event.getEventType() == null || event.getEventType().isBlank()) {
            log.warn("通知事件投递跳过：event 或 eventType 为空, event={}", event);
            return;
        }
        String routingKey = NotificationConstant.ROUTING_KEY_PREFIX + event.getEventType();
        try {
            notificationRabbitTemplate.convertAndSend(
                    NotificationConstant.EXCHANGE, routingKey, event);
            log.info("通知事件已投递: eventType={}, routingKey={}, recipients={}, businessId={}",
                    event.getEventType(), routingKey, event.getRecipientIds(), event.getBusinessId());
        } catch (AmqpException e) {
            // RabbitMQ 未启动/连接失败时仅记日志，不阻塞业务
            log.error("通知事件投递失败（RabbitMQ 不可用）: eventType={}, routingKey={}, error={}",
                    event.getEventType(), routingKey, e.getMessage());
        } catch (Exception e) {
            log.error("通知事件投递异常: eventType={}, error={}",
                    event.getEventType(), e.getMessage(), e);
        }
    }
}
