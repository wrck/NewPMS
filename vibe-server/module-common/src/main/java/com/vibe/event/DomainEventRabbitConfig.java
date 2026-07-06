package com.vibe.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.context.annotation.Primary;

/**
 * 领域事件总线 RabbitMQ 配置
 *
 * <p>声明领域事件使用的 Exchange / Queue / Binding，与 module-system 的通知引擎
 * （{@code vibe.notification.exchange}）隔离，避免事件路由串扰：</p>
 *
 * <pre>
 *   Exchange:  vibe.domain.event.exchange (Topic)
 *   Queue:     vibe.es.sync.queue         (持久化，由 ES 同步消费者消费)
 *   Binding:   vibe.domain.event.* → vibe.es.sync.queue（消费全部领域事件）
 * </pre>
 *
 * <p>说明：与 module-system 的 {@code RabbitMQConfig}（{@code notificationMessageConverter}/
 * {@code notificationRabbitTemplate}）通过 bean 名隔离，互不影响。</p>
 *
 * @author vibe
 */
@Configuration
public class DomainEventRabbitConfig {

    /**
     * 领域事件 Topic Exchange
     */
    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(DomainEventConstant.EXCHANGE, true, false);
    }

    /**
     * ES 同步消费队列（持久化）
     */
    @Bean
    public Queue esSyncQueue() {
        return QueueBuilder.durable(DomainEventConstant.ES_SYNC_QUEUE).build();
    }

    /**
     * 绑定：vibe.domain.event.* → vibe.es.sync.queue
     *
     * <p>ES 同步消费者订阅全部领域事件，按 eventType 决定更新哪个索引。</p>
     */
    @Bean
    public Binding esSyncBinding(Queue esSyncQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(esSyncQueue)
                .to(domainEventExchange)
                .with(DomainEventConstant.ROUTING_KEY_PATTERN_ALL);
    }

    /**
     * 领域事件消息转换器（JSON，支持 Java 8 时间类型）
     *
     * <p>标记 {@code @Primary}：项目中可能存在多个 {@link MessageConverter} Bean
     * （如 module-system 的 {@code notificationMessageConverter}），Spring Boot 自动装配
     * 监听容器工厂时通过 {@code getIfUnique()} 选取 Bean，多候选且无 @Primary 时返回 null，
     * 会导致 {@code @RabbitListener} 无法反序列化 JSON。标记 @Primary 确保监听容器工厂
     * 始终拿到一个 Jackson 转换器，两个消费者（通知 / ES 同步）均使用同一类型转换器。</p>
     */
    @Bean
    @Primary
    public MessageConverter domainEventMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * 领域事件 RabbitTemplate（独立命名，避免与 notificationRabbitTemplate 冲突）
     */
    @Bean
    public RabbitTemplate domainEventRabbitTemplate(ConnectionFactory connectionFactory,
                                                     MessageConverter domainEventMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(domainEventMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
