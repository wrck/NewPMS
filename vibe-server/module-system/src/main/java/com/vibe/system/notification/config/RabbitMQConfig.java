package com.vibe.system.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vibe.system.notification.NotificationConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RabbitMQ 配置（消息通知引擎）
 *
 * <p>声明通知引擎使用的 Exchange / Queue / Binding：</p>
 * <pre>
 *   Exchange:  vibe.notification.exchange (Topic)
 *   Queue:     vibe.notification.queue   (持久化)
 *   Binding:   vibe.notification.* → vibe.notification.queue
 * </pre>
 *
 * <p>同时提供 {@link Jackson2JsonMessageConverter} 与 {@link RestTemplate} Bean，
 * 供 {@code NotificationProducer} 与各渠道适配器使用。</p>
 *
 * <p>说明：Exchange/Queue 用 {@code durable=true} 声明，broker 重启后不丢失；
 * Queue 用 {@code durable=true} + AMQP auto-delete=false，由 broker 自动创建。
 * 生产环境建议在 broker 预先创建，避免首次启动因声明冲突失败。</p>
 *
 * @author vibe
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 通知 Topic Exchange
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NotificationConstant.EXCHANGE, true, false);
    }

    /**
     * 通知队列（持久化）
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NotificationConstant.QUEUE).build();
    }

    /**
     * 绑定：vibe.notification.* → vibe.notification.queue
     *
     * <p>routing key 形如 vibe.notification.TASK_ASSIGNED，
     * 通过 pattern vibe.notification.* 匹配所有事件类型，统一路由到同一队列消费。</p>
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(NotificationConstant.ROUTING_KEY_PATTERN);
    }

    /**
     * 消息转换器：使用 JSON 序列化，POJO 可直接发送/接收
     */
    @Bean
    public MessageConverter notificationMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * RabbitTemplate（使用 JSON 转换器）
     */
    @Bean
    public RabbitTemplate notificationRabbitTemplate(ConnectionFactory connectionFactory,
                                                     MessageConverter notificationMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(notificationMessageConverter);
        // mandatory=true：消息无法路由时返回 BasicReturn，配合 publisher-returns 监听
        template.setMandatory(true);
        return template;
    }

    /**
     * RestTemplate（渠道适配器调用飞书/钉钉 Webhook 用）
     */
    @Bean
    public RestTemplate notificationRestTemplate() {
        return new RestTemplate();
    }
}
