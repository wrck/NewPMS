package com.vibe.system.notification.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件渠道适配器
 *
 * <p>MVP 阶段为占位实现，仅记录日志模拟发送。
 * 后续接入真实邮件服务时，注入 {@code JavaMailSender} 实现发送逻辑。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
public class EmailAdapter {

    /**
     * 发送邮件（占位实现）。
     *
     * @param email   接收人邮箱（可为 null，无邮箱映射时跳过）
     * @param title   邮件标题
     * @param content 邮件正文
     */
    public void send(String email, String title, String content) {
        if (email == null || email.isBlank()) {
            log.info("邮件通知跳过（邮箱为空）: title={}, content={}", title, content);
            return;
        }
        // TODO: 接入 JavaMailSender 实现真实邮件发送
        log.info("[邮件通知-模拟] email={}, title={}, content={}", email, title, content);
    }
}
