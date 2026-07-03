package com.vibe.system.notification.consumer;

import com.rabbitmq.client.Channel;
import com.vibe.system.entity.SysNoticeTemplateEntity;
import com.vibe.system.notification.NotificationConstant;
import com.vibe.system.notification.NotificationEvent;
import com.vibe.system.notification.NotificationRecipientType;
import com.vibe.system.notification.renderer.NotificationTemplateRenderer;
import com.vibe.system.notification.router.NotificationChannelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 通知事件消费者
 *
 * <p>监听 {@code vibe.notification.queue}，消费流程：</p>
 * <ol>
 *   <li>接收 {@link NotificationEvent} 消息</li>
 *   <li>按 templateCode 加载模板（Redis 缓存 / DB）</li>
 *   <li>渲染标题和内容（变量替换）</li>
 *   <li>渠道路由：根据接收人类型选择渠道，频率控制，调用适配器发送</li>
 *   <li>站内信由 {@link NotificationChannelRouter} 调用 InAppAdapter 写入 sys_notice 表</li>
 * </ol>
 *
 * <p>消费失败容错策略：</p>
 * <ul>
 *   <li>业务异常（模板缺失、渲染失败等）：记日志后 ACK 丢弃，避免无限重试堆积</li>
 *   <li>消息本身问题（反序列化失败）：reject 不重入队列</li>
 *   <li>消费者配置 acknowledge-mode: manual，由本类显式 ACK/NACK</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationTemplateRenderer templateRenderer;
    private final NotificationChannelRouter channelRouter;

    /**
     * 消费通知事件。
     *
     * <p>消费失败仅记日志，不抛异常（业务异常 ACK 丢弃；不阻塞业务）。</p>
     *
     * @param event    通知事件（JSON 反序列化）
     * @param message  原始消息（用于获取 deliveryTag）
     * @param channel  RabbitMQ Channel（用于手动 ACK）
     * @param tag      delivery tag
     */
    @RabbitListener(queues = NotificationConstant.QUEUE)
    public void consume(@Payload NotificationEvent event,
                        Message message,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        if (event == null) {
            log.warn("收到空通知事件，直接 ACK 丢弃");
            ack(channel, tag);
            return;
        }
        log.info("收到通知事件: eventType={}, templateCode={}, recipients={}, businessId={}",
                event.getEventType(), event.getTemplateCode(), event.getRecipientIds(), event.getBusinessId());
        try {
            process(event);
        } catch (Exception e) {
            // 消费失败仅记日志，不抛异常，ACK 丢弃避免无限重试
            log.error("通知事件消费失败（ACK 丢弃）: eventType={}, error={}",
                    event.getEventType(), e.getMessage(), e);
        } finally {
            ack(channel, tag);
        }
    }

    /**
     * 实际消费处理逻辑。
     */
    private void process(NotificationEvent event) {
        // 1. 加载模板
        String templateCode = event.getTemplateCode();
        if (templateCode == null || templateCode.isBlank()) {
            templateCode = event.getEventType();
        }
        SysNoticeTemplateEntity template = templateRenderer.loadTemplate(templateCode);
        if (template == null) {
            log.warn("通知模板不存在或已禁用，跳过: templateCode={}", templateCode);
            return;
        }

        // 2. 渲染标题/内容
        NotificationTemplateRenderer.RenderedContent rendered =
                templateRenderer.render(template, event.getVariables());
        if (rendered == null) {
            log.warn("通知模板渲染结果为空，跳过: templateCode={}", templateCode);
            return;
        }

        // 3. 解析接收人类型
        NotificationRecipientType recipientType = NotificationRecipientType.parse(event.getRecipientType());
        if (recipientType == null) {
            // 默认按内部员工处理
            recipientType = NotificationRecipientType.INTERNAL;
        }

        // 4. 渠道路由 + 多渠道发送（含频率控制）
        List<Long> recipientIds = event.getRecipientIds();
        if (recipientIds == null || recipientIds.isEmpty()) {
            log.info("通知事件无接收人，仅按渠道发送（飞书群发等）: eventType={}, channels={}",
                    event.getEventType(), rendered.channels());
            // 无接收人时仍发送飞书/钉钉群消息（接收人传 null）
            channelRouter.route(null, recipientType, event.getEventType(),
                    rendered.channels(), rendered.title(), rendered.content());
            return;
        }
        channelRouter.routeBatch(recipientIds, recipientType, event.getEventType(),
                rendered.channels(), rendered.title(), rendered.content());
    }

    /**
     * 手动 ACK（异常时仅记日志，不抛出，避免消费者死循环）。
     */
    private void ack(Channel channel, long tag) {
        try {
            channel.basicAck(tag, false);
        } catch (IOException e) {
            log.error("RabbitMQ ACK 失败: tag={}, error={}", tag, e.getMessage(), e);
        }
    }
}
