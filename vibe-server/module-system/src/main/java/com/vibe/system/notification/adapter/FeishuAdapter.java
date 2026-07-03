package com.vibe.system.notification.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书渠道适配器
 *
 * <p>调用飞书自定义机器人 Webhook 发送卡片消息（interactive card）。</p>
 *
 * <p>Webhook URL 获取优先级：</p>
 * <ol>
 *   <li>sys_config 表中 configKey = {@code notification.feishu.webhook} 的值</li>
 *   <li>application.yml 中 {@code vibe.notification.feishu.webhook} 配置</li>
 * </ol>
 *
 * <p>说明：MVP 阶段 Webhook 未配置时仅记日志，不报错。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
public class FeishuAdapter {

    @Autowired(required = false)
    private RestTemplate notificationRestTemplate;

    @Value("${vibe.notification.feishu.webhook:}")
    private String defaultWebhookUrl;

    /**
     * 发送飞书卡片消息。
     *
     * @param webhookUrl 飞书机器人 Webhook URL（为空则使用配置默认值）
     * @param title      消息标题
     * @param content    消息内容
     */
    public void send(String webhookUrl, String title, String content) {
        String url = (webhookUrl == null || webhookUrl.isBlank()) ? defaultWebhookUrl : webhookUrl;
        if (url == null || url.isBlank()) {
            log.info("飞书通知跳过（Webhook 未配置）: title={}, content={}", title, content);
            return;
        }
        if (notificationRestTemplate == null) {
            log.warn("飞书通知跳过（RestTemplate 未初始化）: title={}", title);
            return;
        }
        try {
            Map<String, Object> card = new HashMap<>(2);
            Map<String, Object> header = new HashMap<>(2);
            Map<String, Object> headerTitle = new HashMap<>(2);
            headerTitle.put("tag", "plain_text");
            headerTitle.put("content", title == null ? "" : title);
            header.put("title", headerTitle);
            card.put("header", header);

            Map<String, Object> element = new HashMap<>(2);
            Map<String, Object> elementContent = new HashMap<>(2);
            elementContent.put("tag", "plain_text");
            elementContent.put("content", content == null ? "" : content);
            element.put("text", elementContent);
            element.put("tag", "div");
            card.put("elements", new Object[]{element});

            Map<String, Object> body = new HashMap<>(2);
            body.put("msg_type", "interactive");
            body.put("card", card);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = notificationRestTemplate.postForEntity(url, request, String.class);
            log.info("飞书通知发送完成: title={}, respStatus={}, respBody={}",
                    title, resp.getStatusCode(), resp.getBody());
        } catch (Exception e) {
            log.error("飞书通知发送失败: title={}, error={}", title, e.getMessage(), e);
        }
    }
}
