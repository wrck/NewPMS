package com.vibe.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 基于 RabbitMQ 的领域事件发布者
 *
 * <p>将领域事件投递到 RabbitMQ Exchange {@code vibe.domain.event.exchange}（Topic），
 * routing key 形如 {@code vibe.domain.event.{eventType}}，如 {@code vibe.domain.event.PROJECT_CREATED}。</p>
 *
 * <p>容错策略：投递失败仅记日志、不抛异常，避免阻塞业务流程。
 * RabbitMQ 在开发阶段可暂不启动，业务仍可正常运行（ES 同步降级为缺失，由 MySQL 兜底检索）。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
public class RabbitMqDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate domainEventRabbitTemplate;

    public RabbitMqDomainEventPublisher(
            @Qualifier("domainEventRabbitTemplate") RabbitTemplate domainEventRabbitTemplate) {
        this.domainEventRabbitTemplate = domainEventRabbitTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null || event.getEventType() == null || event.getEventType().isBlank()) {
            log.warn("领域事件投递跳过：event 或 eventType 为空, event={}", event);
            return;
        }
        String routingKey = DomainEventConstant.ROUTING_KEY_PREFIX + event.getEventType();
        try {
            domainEventRabbitTemplate.convertAndSend(
                    DomainEventConstant.EXCHANGE, routingKey, event);
            log.info("领域事件已投递: eventType={}, routingKey={}, businessKey={}",
                    event.getEventType(), routingKey, event.getBusinessKey());
        } catch (AmqpException e) {
            // RabbitMQ 未启动/连接失败时仅记日志，不阻塞业务
            log.error("领域事件投递失败（RabbitMQ 不可用）: eventType={}, routingKey={}, error={}",
                    event.getEventType(), routingKey, e.getMessage());
        } catch (Exception e) {
            log.error("领域事件投递异常: eventType={}, error={}",
                    event.getEventType(), e.getMessage(), e);
        }
    }
}
