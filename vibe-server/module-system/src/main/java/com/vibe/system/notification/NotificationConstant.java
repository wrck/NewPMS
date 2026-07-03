package com.vibe.system.notification;

/**
 * 消息通知引擎常量
 *
 * <p>集中声明事件类型、渠道、Redis Key、Exchange/Queue 等常量，
 * 避免散落在各业务类中造成冲突。</p>
 *
 * @author vibe
 */
public final class NotificationConstant {

    private NotificationConstant() {
    }

    /* ============ RabbitMQ 基础设施 ============ */
    /** 通知 Exchange */
    public static final String EXCHANGE = "vibe.notification.exchange";
    /** 通知队列 */
    public static final String QUEUE = "vibe.notification.queue";
    /** routing key 前缀（完整 key = PREFIX + eventType） */
    public static final String ROUTING_KEY_PREFIX = "vibe.notification.";
    /** binding pattern（匹配 vibe.notification.{eventType}） */
    public static final String ROUTING_KEY_PATTERN = "vibe.notification.*";

    /* ============ 事件类型（与 sys_notice_template.template_code 一致） ============ */
    public static final String EVENT_TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String EVENT_TASK_REMINDER = "TASK_REMINDER";
    public static final String EVENT_TASK_OVERDUE = "TASK_OVERDUE";
    public static final String EVENT_DELIVERABLE_REVIEW = "DELIVERABLE_REVIEW";
    public static final String EVENT_DELIVERABLE_RETURNED = "DELIVERABLE_RETURNED";
    public static final String EVENT_DELIVERABLE_CONFIRMED = "DELIVERABLE_CONFIRMED";
    public static final String EVENT_DEVICE_ARRIVED = "DEVICE_ARRIVED";
    public static final String EVENT_DEVICE_ABNORMAL = "DEVICE_ABNORMAL";
    public static final String EVENT_RISK_WARNING = "RISK_WARNING";

    /* ============ 渠道编码（与 sys_notice_template.channels JSON 数组一致） ============ */
    public static final String CHANNEL_FEISHU = "FEISHU";
    public static final String CHANNEL_DINGTALK = "DINGTALK";
    public static final String CHANNEL_SITE = "SITE";
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_EMAIL = "EMAIL";

    /* ============ 接收人类型 ============ */
    public static final String RECIPIENT_INTERNAL = "INTERNAL";
    public static final String RECIPIENT_AGENT = "AGENT";
    public static final String RECIPIENT_CUSTOMER = "CUSTOMER";

    /* ============ sys_config 配置键（飞书/钉钉 Webhook） ============ */
    public static final String CONFIG_KEY_FEISHU_WEBHOOK = "notification.feishu.webhook";
    public static final String CONFIG_KEY_DINGTALK_WEBHOOK = "notification.dingtalk.webhook";
    public static final String CONFIG_KEY_FEISHU_SECRET = "notification.feishu.secret";
    public static final String CONFIG_KEY_DINGTALK_SECRET = "notification.dingtalk.secret";

    /* ============ Redis Key ============ */
    /** 模板缓存 key 前缀，完整 key = PREFIX + templateCode */
    public static final String REDIS_KEY_TEMPLATE_PREFIX = "vibe:notification:template:";
    /** 频率控制 key 前缀，完整 key = PREFIX + recipientId + ":" + eventType */
    public static final String REDIS_KEY_FREQ_PREFIX = "vibe:notification:freq:";

    /* ============ TTL ============ */
    /** 模板缓存 TTL（30 分钟，秒） */
    public static final long TEMPLATE_TTL_SECONDS = 30 * 60L;
    /** 频率控制 TTL（5 分钟，秒） */
    public static final long FREQ_TTL_SECONDS = 5 * 60L;

    /* ============ 业务类型（businessType 字段取值） ============ */
    public static final String BIZ_PROJECT_TASK = "PROJECT_TASK";
    public static final String BIZ_OUTSOURCE_TASK = "OUTSOURCE_TASK";
    public static final String BIZ_DEVICE = "DEVICE";
    public static final String BIZ_PROJECT_RISK = "PROJECT_RISK";
}
