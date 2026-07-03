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
 * 钉钉渠道适配器
 *
 * <p>调用钉钉自定义机器人 Webhook 发送 markdown 消息。</p>
 *
 * <p>Webhook URL 获取优先级：</p>
 * <ol>
 *   <li>sys_config 表中 configKey = {@code notification.dingtalk.webhook} 的值</li>
 *   <li>application.yml 中 {@code vibe.notification.dingtalk.webhook} 配置</li>
 * </ol>
 *
 * <p>说明：MVP 阶段 Webhook 未配置时仅记日志，不报错。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
public class DingTalkAdapter {

    @Autowired(required = false)
    private RestTemplate notificationRestTemplate;

    @Value("${vibe.notification.dingtalk.webhook:}")
    private String defaultWebhookUrl;

    /**
     * 发送钉钉 markdown 消息。
     *
     * @param webhookUrl 钉钉机器人 Webhook URL（为空则使用配置默认值）
     * @param title      消息标题
     * @param content    消息内容
     */
    public void send(String webhookUrl, String title, String content) {
        String url = (webhookUrl == null || webhookUrl.isBlank()) ? defaultWebhookUrl : webhookUrl;
        if (url == null || url.isBlank()) {
            log.info("钉钉通知跳过（Webhook 未配置）: title={}, content={}", title, content);
            return;
        }
        if (notificationRestTemplate == null) {
            log.warn("钉钉通知跳过（RestTemplate 未初始化）: title={}", title);
            return;
        }
        try {
            Map<String, Object> markdown = new HashMap<>(2);
            markdown.put("title", title == null ? "" : title);
            markdown.put("text", content == null ? "" : content);

            Map<String, Object> body = new HashMap<>(2);
            body.put("msgtype", "markdown");
            body.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = notificationRestTemplate.postForEntity(url, request, String.class);
            log.info("钉钉通知发送完成: title={}, respStatus={}, respBody={}",
                    title, resp.getStatusCode(), resp.getBody());
        } catch (Exception e) {
            log.error("钉钉通知发送失败: title={}, error={}", title, e.getMessage(), e);
        }
    }
}
