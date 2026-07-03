package com.vibe.system.notification.renderer;

import com.vibe.system.entity.SysNoticeTemplateEntity;
import com.vibe.system.mapper.SysNoticeTemplateMapper;
import com.vibe.system.notification.NotificationConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知模板渲染器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>按 templateCode 从 Redis 缓存加载模板，缓存未命中则回源 DB 并回写缓存</li>
 *   <li>变量替换：将标题/内容模板中的 {@code ${variableName}} 占位符替换为 variables 中的值</li>
 *   <li>渲染标题和内容，返回渲染后的 {@link RenderedContent}</li>
 * </ul>
 *
 * <p>缓存 Key：{@code vibe:notification:template:{templateCode}}，TTL 30 分钟。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTemplateRenderer {

    /** 变量占位符正则：匹配 ${varName}，varName 为字母数字下划线 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    private static final Duration TEMPLATE_TTL = Duration.ofSeconds(NotificationConstant.TEMPLATE_TTL_SECONDS);

    private final RedisTemplate<String, Object> redisTemplate;
    private final SysNoticeTemplateMapper sysNoticeTemplateMapper;

    /**
     * 按 templateCode 加载模板（带 Redis 缓存）。
     *
     * @return 模板实体；不存在或被禁用时返回 null
     */
    public SysNoticeTemplateEntity loadTemplate(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return null;
        }
        String cacheKey = NotificationConstant.REDIS_KEY_TEMPLATE_PREFIX + templateCode;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof SysNoticeTemplateEntity template) {
                return template;
            }
        } catch (Exception e) {
            log.warn("模板缓存读取失败,降级回源DB: templateCode={}, error={}", templateCode, e.getMessage());
        }

        // 回源 DB
        SysNoticeTemplateEntity template = sysNoticeTemplateMapper.selectByTemplateCode(templateCode);
        if (template == null) {
            log.warn("通知模板不存在: templateCode={}", templateCode);
            return null;
        }
        // 禁用模板不返回
        if (template.getStatus() == null || template.getStatus() != 1) {
            log.warn("通知模板已禁用: templateCode={}, status={}", templateCode, template.getStatus());
            return null;
        }
        // 回写缓存（失败不影响主流程）
        try {
            redisTemplate.opsForValue().set(cacheKey, template, TEMPLATE_TTL);
        } catch (Exception e) {
            log.warn("模板缓存写入失败: templateCode={}, error={}", templateCode, e.getMessage());
        }
        return template;
    }

    /**
     * 清除指定模板的缓存（模板更新时调用）。
     */
    public void evictCache(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(NotificationConstant.REDIS_KEY_TEMPLATE_PREFIX + templateCode);
        } catch (Exception e) {
            log.warn("模板缓存清除失败: templateCode={}, error={}", templateCode, e.getMessage());
        }
    }

    /**
     * 渲染模板。
     *
     * @param template 模板实体
     * @param variables 变量 Map（可为 null）
     * @return 渲染后的标题/内容
     */
    public RenderedContent render(SysNoticeTemplateEntity template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        String title = replaceVariables(template.getTitleTemplate(), variables);
        String content = replaceVariables(template.getContentTemplate(), variables);
        return new RenderedContent(title, content, template.getChannels());
    }

    /**
     * 将模板字符串中的 ${varName} 替换为 variables 中对应值。
     * 未提供的变量替换为空字符串，避免占位符残留。
     */
    private String replaceVariables(String tpl, Map<String, String> variables) {
        if (tpl == null || tpl.isEmpty()) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            // 无变量：直接清除所有占位符
            return VARIABLE_PATTERN.matcher(tpl).replaceAll("");
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(tpl);
        StringBuilder sb = new StringBuilder(tpl.length() + 32);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = variables.get(key);
            matcher.appendReplacement(sb, val == null ? "" : Matcher.quoteReplacement(val));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 渲染结果。
     *
     * @param title   渲染后的标题
     * @param content 渲染后的内容
     * @param channels 模板配置的渠道 JSON 数组字符串（如 ["FEISHU","SITE"]）
     */
    public record RenderedContent(String title, String content, String channels) {
    }
}
