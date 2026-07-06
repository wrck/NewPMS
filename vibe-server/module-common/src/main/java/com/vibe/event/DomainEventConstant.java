package com.vibe.event;

/**
 * 领域事件总线常量
 *
 * <p>集中声明领域事件使用的 RabbitMQ Exchange、routing key 前缀等基础设施常量，
 * 避免散落在各业务类造成冲突。事件投递到 {@code vibe.domain.event.exchange}（Topic），
 * routing key 形如 {@code vibe.domain.event.{eventType}}（如 {@code vibe.domain.event.PROJECT_CREATED}）。</p>
 *
 * <p>下游消费者（如 ES 同步、通知引擎）按需订阅对应 pattern：
 * <ul>
 *   <li>消费全部事件：{@code vibe.domain.event.*}</li>
 *   <li>仅消费项目相关事件：{@code vibe.domain.event.PROJECT_*}</li>
 *   <li>仅消费工单完成事件：{@code vibe.domain.event.WORK_ORDER_COMPLETED}</li>
 * </ul>
 *
 * @author vibe
 */
public final class DomainEventConstant {

    private DomainEventConstant() {
    }

    /** 领域事件 Topic Exchange */
    public static final String EXCHANGE = "vibe.domain.event.exchange";
    /** 领域事件队列（ES 同步消费者） */
    public static final String ES_SYNC_QUEUE = "vibe.es.sync.queue";
    /** routing key 前缀（完整 key = PREFIX + eventType） */
    public static final String ROUTING_KEY_PREFIX = "vibe.domain.event.";
    /** binding pattern：匹配全部领域事件 */
    public static final String ROUTING_KEY_PATTERN_ALL = "vibe.domain.event.*";
    /** binding pattern：匹配项目相关事件 */
    public static final String ROUTING_KEY_PATTERN_PROJECT = "vibe.domain.event.PROJECT_*";
    /** binding pattern：匹配设备相关事件 */
    public static final String ROUTING_KEY_PATTERN_DEVICE = "vibe.domain.event.DEVICE_*";
    /** binding pattern：匹配工单相关事件 */
    public static final String ROUTING_KEY_PATTERN_WORK_ORDER = "vibe.domain.event.WORK_ORDER_*";

    /* ============ 事件类型 ============ */
    public static final String EVENT_PROJECT_CREATED = "PROJECT_CREATED";
    public static final String EVENT_PROJECT_STATUS_CHANGED = "PROJECT_STATUS_CHANGED";
    public static final String EVENT_TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String EVENT_TASK_COMPLETED = "TASK_COMPLETED";
    public static final String EVENT_DEVICE_STATUS_CHANGED = "DEVICE_STATUS_CHANGED";
    public static final String EVENT_INVENTORY_WARNING = "INVENTORY_WARNING";
    public static final String EVENT_WORK_ORDER_COMPLETED = "WORK_ORDER_COMPLETED";
    public static final String EVENT_DELIVERABLE_SUBMITTED = "DELIVERABLE_SUBMITTED";
    public static final String EVENT_DELIVERABLE_REVIEWED = "DELIVERABLE_REVIEWED";
    public static final String EVENT_ACCEPTANCE_PASSED = "ACCEPTANCE_PASSED";
    public static final String EVENT_CUTOVER_APPROVED = "CUTOVER_APPROVED";
    public static final String EVENT_CHANGE_APPROVED = "CHANGE_APPROVED";
    public static final String EVENT_RISK_ESCALATED = "RISK_ESCALATED";
    public static final String EVENT_AGENT_SCORED = "AGENT_SCORED";
    public static final String EVENT_NOTICE_SENT = "NOTICE_SENT";
}
