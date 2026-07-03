package com.vibe.system.notification;

/**
 * 通知接收人类型
 *
 * <p>用于渠道路由：</p>
 * <ul>
 *   <li>{@link #INTERNAL} - 内部员工，走飞书/钉钉 + 站内信</li>
 *   <li>{@link #AGENT} - 代理商，走飞书/钉钉 + 站内信</li>
 *   <li>{@link #CUSTOMER} - 客户，走短信/邮件</li>
 * </ul>
 *
 * @author vibe
 */
public enum NotificationRecipientType {

    /** 内部员工 */
    INTERNAL,
    /** 代理商 */
    AGENT,
    /** 客户 */
    CUSTOMER;

    /**
     * 解析接收人类型字符串（忽略大小写），非法时返回 null。
     */
    public static NotificationRecipientType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return NotificationRecipientType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
