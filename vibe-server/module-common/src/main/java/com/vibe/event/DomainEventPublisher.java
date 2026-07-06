package com.vibe.event;

/**
 * 领域事件发布者接口
 *
 * <p>业务模块在关键节点调用 {@link #publish(DomainEvent)} 投递事件到事件总线
 * （默认实现为 RabbitMQ）。投递应当是异步、容错的，不应阻塞业务事务。</p>
 *
 * @author vibe
 */
public interface DomainEventPublisher {

    /**
     * 投递领域事件。
     *
     * @param event 领域事件
     */
    void publish(DomainEvent event);
}
