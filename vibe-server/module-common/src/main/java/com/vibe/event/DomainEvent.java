package com.vibe.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * 领域事件抽象基类
 *
 * <p>所有跨模块业务事件的根。事件采用「事件溯源 + 最终一致性」思路：
 * 业务方在关键节点调用 {@link DomainEventPublisher#publish(DomainEvent)} 将事件投递到
 * RabbitMQ Exchange {@code vibe.domain.event.exchange}，下游消费者（如 ES 同步、
 * 通知引擎、BI 统计）按 routing key 订阅事件。</p>
 *
 * <p>核心字段：</p>
 * <ul>
 *   <li>{@code eventId}：事件唯一 ID（UUID），用于幂等去重</li>
 *   <li>{@code eventType}：事件类型（如 PROJECT_CREATED），同时作为 RabbitMQ routing key 后缀</li>
 *   <li>{@code timestamp}：事件发生时间</li>
 *   <li>{@code businessKey}：业务主键（如 projectId/deviceId/workOrderId），便于消费者定位数据</li>
 *   <li>{@code data}：业务数据（JSON 字符串或 Map），由子类填充业务字段</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "领域事件基类")
public abstract class DomainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件唯一 ID（UUID，用于幂等去重） */
    @Schema(description = "事件唯一 ID")
    private String eventId;

    /** 事件类型（如 PROJECT_CREATED），同时作为 RabbitMQ routing key 后缀 */
    @Schema(description = "事件类型")
    private String eventType;

    /** 事件发生时间 */
    @Schema(description = "事件发生时间")
    private Instant timestamp;

    /** 业务主键（如 projectId/deviceId/workOrderId） */
    @Schema(description = "业务主键")
    private String businessKey;

    /** 业务数据（JSON 字符串或 Map，由子类填充） */
    @Schema(description = "业务数据")
    private Object data;

    /**
     * 子类构造方法：自动填充 eventId/eventType/timestamp。
     *
     * @param eventType   事件类型
     * @param businessKey 业务主键
     */
    protected DomainEvent(String eventType, String businessKey) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.businessKey = businessKey;
    }
}
