package com.vibe.system.notification.router;

import com.vibe.system.notification.NotificationConstant;
import com.vibe.system.notification.NotificationRecipientType;
import com.vibe.system.notification.adapter.DingTalkAdapter;
import com.vibe.system.notification.adapter.EmailAdapter;
import com.vibe.system.notification.adapter.FeishuAdapter;
import com.vibe.system.notification.adapter.InAppAdapter;
import com.vibe.system.notification.adapter.SmsAdapter;
import com.vibe.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 通知渠道路由器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>根据接收人类型选择渠道：
 *     <ul>
 *       <li>INTERNAL / AGENT → 飞书 + 站内信（模板配置了 DINGTALK/SMS/EMAIL 时按模板配置）</li>
 *       <li>CUSTOMER → 短信 + 邮件</li>
 *     </ul>
 *   </li>
 *   <li>频率控制：同一接收人同一事件 5 分钟内不重复发送
 *     （Redis key: {@code vibe:notification:freq:{recipientId}:{eventType}}，TTL 5 分钟）</li>
 *   <li>调用各渠道适配器实际发送消息</li>
 * </ul>
 *
 * <p>渠道选择规则：模板配置的 channels 优先，未配置则按接收人类型默认路由。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationChannelRouter {

    /** 渠道 JSON 数组解析正则：从 ["FEISHU","SITE"] 中提取渠道编码 */
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("\"([A-Z_]+)\"");

    private static final Duration FREQ_TTL = Duration.ofSeconds(NotificationConstant.FREQ_TTL_SECONDS);

    private final RedisTemplate<String, Object> redisTemplate;
    private final SysConfigService sysConfigService;
    private final FeishuAdapter feishuAdapter;
    private final DingTalkAdapter dingTalkAdapter;
    private final InAppAdapter inAppAdapter;
    private final SmsAdapter smsAdapter;
    private final EmailAdapter emailAdapter;

    /**
     * 对单个接收人执行多渠道发送。
     *
     * @param recipientId   接收人 ID
     * @param recipientType 接收人类型
     * @param eventType     事件类型（用于频率控制）
     * @param channelsJson  模板配置的渠道 JSON 数组字符串，如 ["FEISHU","SITE"]；为空则按接收人类型默认
     * @param title         渲染后的标题
     * @param content       渲染后的内容
     */
    public void route(Long recipientId, NotificationRecipientType recipientType,
                      String eventType, String channelsJson, String title, String content) {
        if (recipientId == null) {
            log.warn("渠道路由跳过：recipientId 为空, eventType={}, title={}", eventType, title);
            return;
        }
        // 频率控制：同一接收人同一事件 5 分钟内不重复
        if (!acquireFreqLock(recipientId, eventType)) {
            log.info("频率控制：跳过重复通知, recipientId={}, eventType={}", recipientId, eventType);
            return;
        }

        Set<String> channels = parseChannels(channelsJson);
        if (channels.isEmpty()) {
            // 模板未配置渠道，按接收人类型默认路由
            channels = defaultChannels(recipientType);
        }

        // 客户渠道需要手机号/邮箱，MVP 阶段无映射，暂用 null（适配器内部会跳过）
        String phone = null;
        String email = null;

        for (String channel : channels) {
            try {
                switch (channel) {
                    case NotificationConstant.CHANNEL_FEISHU -> feishuAdapter.send(
                            sysConfigService.getConfigValue(NotificationConstant.CONFIG_KEY_FEISHU_WEBHOOK),
                            title, content);
                    case NotificationConstant.CHANNEL_DINGTALK -> dingTalkAdapter.send(
                            sysConfigService.getConfigValue(NotificationConstant.CONFIG_KEY_DINGTALK_WEBHOOK),
                            title, content);
                    case NotificationConstant.CHANNEL_SITE -> inAppAdapter.send(recipientId, title, content);
                    case NotificationConstant.CHANNEL_SMS -> smsAdapter.send(phone, title, content);
                    case NotificationConstant.CHANNEL_EMAIL -> emailAdapter.send(email, title, content);
                    default -> log.warn("未知渠道编码，跳过: channel={}, eventType={}", channel, eventType);
                }
            } catch (Exception e) {
                // 单个渠道发送失败不影响其他渠道
                log.error("渠道发送失败: channel={}, recipientId={}, error={}",
                        channel, recipientId, e.getMessage(), e);
            }
        }
    }

    /**
     * 频率控制：同一接收人同一事件 5 分钟内仅发送一次。
     *
     * @return true 表示获得发送许可；false 表示被频率控制拦截
     */
    private boolean acquireFreqLock(Long recipientId, String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return true;
        }
        String key = NotificationConstant.REDIS_KEY_FREQ_PREFIX + recipientId + ":" + eventType;
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", FREQ_TTL);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            // Redis 异常时放行，避免阻塞通知发送
            log.warn("频率控制 Redis 异常，放行: recipientId={}, eventType={}, error={}",
                    recipientId, eventType, e.getMessage());
            return true;
        }
    }

    /**
     * 解析模板 channels JSON 数组字符串，返回渠道编码集合。
     * 输入示例：["FEISHU","SITE"]
     */
    private Set<String> parseChannels(String channelsJson) {
        Set<String> channels = new java.util.HashSet<>();
        if (channelsJson == null || channelsJson.isBlank()) {
            return channels;
        }
        java.util.regex.Matcher m = CHANNEL_PATTERN.matcher(channelsJson);
        while (m.find()) {
            channels.add(m.group(1));
        }
        return channels;
    }

    /**
     * 按接收人类型返回默认渠道。
     */
    private Set<String> defaultChannels(NotificationRecipientType recipientType) {
        if (recipientType == NotificationRecipientType.CUSTOMER) {
            return Set.of(NotificationConstant.CHANNEL_SMS, NotificationConstant.CHANNEL_EMAIL);
        }
        // INTERNAL / AGENT 默认飞书 + 站内信
        return Set.of(NotificationConstant.CHANNEL_FEISHU, NotificationConstant.CHANNEL_SITE);
    }

    /**
     * 批量频率控制清理（测试/管理用）。
     */
    public void clearFreqLock(Long recipientId, String eventType) {
        if (recipientId == null || eventType == null) {
            return;
        }
        try {
            redisTemplate.delete(NotificationConstant.REDIS_KEY_FREQ_PREFIX + recipientId + ":" + eventType);
        } catch (Exception e) {
            log.warn("频率控制清理失败: recipientId={}, eventType={}, error={}",
                    recipientId, eventType, e.getMessage());
        }
    }

    /**
     * 用于消费者批量处理时的接收人列表遍历辅助。
     */
    public void routeBatch(List<Long> recipientIds, NotificationRecipientType recipientType,
                           String eventType, String channelsJson, String title, String content) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            log.info("批量渠道路由跳过：recipientIds 为空, eventType={}", eventType);
            return;
        }
        for (Long recipientId : recipientIds) {
            route(recipientId, recipientType, eventType, channelsJson, title, content);
        }
    }
}
