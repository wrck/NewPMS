package com.vibe.system.notification.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信渠道适配器
 *
 * <p>MVP 阶段为占位实现，仅记录日志模拟发送。
 * 后续接入真实短信服务商（阿里云/腾讯云 SMS）时，在本类中实现 HTTP 调用。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
public class SmsAdapter {

    /**
     * 发送短信（占位实现）。
     *
     * @param phone   接收人手机号（可为 null，MVP 阶段无客户手机号映射时跳过）
     * @param title   通知标题
     * @param content 通知内容
     */
    public void send(String phone, String title, String content) {
        if (phone == null || phone.isBlank()) {
            log.info("短信通知跳过（手机号为空）: title={}, content={}", title, content);
            return;
        }
        // TODO: 接入真实短信服务商 SDK（阿里云/腾讯云 SMS）
        log.info("[短信通知-模拟] phone={}, title={}, content={}", phone, title, content);
    }
}
