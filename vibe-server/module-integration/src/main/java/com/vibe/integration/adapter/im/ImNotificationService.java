package com.vibe.integration.adapter.im;

import com.vibe.integration.adapter.im.dto.ImMessageDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * IM 通知转发 Service
 *
 * <p>调用 {@link ImNotificationFeignClient} 转发内部通知到 IM 系统（飞书/钉钉/企业微信）。</p>
 *
 * <p>通过 {@code @CircuitBreaker(name = "im-adapter")} + {@code @Retry(name = "im-adapter")}
 * 实现 Resilience4j 熔断与重试：30% 失败率即熔断（IM 系统对发送成功率要求高），5 分钟半开试探，最多 2 次重试。</p>
 *
 * <p>当 {@code integration.im.enabled=false}（默认）时所有发送方法返回 {@code null}，
 * 表示消息未实际投递。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImNotificationService {

    private final ImNotificationFeignClient imNotificationFeignClient;

    @Value("${integration.im.enabled:false}")
    private boolean imEnabled;

    /**
     * 发送 IM 通知消息。
     *
     * <p>熔断开启时返回 {@code null} 表示未实际投递，调用方应记录到本地"待重发队列"。</p>
     *
     * @param message IM 消息体
     * @return 发送结果（含 IM 侧 msgId）；未启用或熔断时返回 {@code null}
     */
    @CircuitBreaker(name = "im-adapter", fallbackMethod = "sendNotificationFallback")
    @Retry(name = "im-adapter")
    public ImMessageDTO sendNotification(ImMessageDTO message) {
        if (!imEnabled) {
            log.warn("IM 通知转发未启用（integration.im.enabled=false），跳过 sendNotification bizRefId={}",
                    message.getBizRefId());
            return null;
        }
        log.info("发送 IM 通知 platform={} receivers={} bizRefId={}",
                message.getPlatform(), message.getReceiverIds(), message.getBizRefId());
        return imNotificationFeignClient.sendNotification(message);
    }

    /* ============ 降级方法 ============ */

    /**
     * sendNotification 熔断/失败降级：返回 {@code null} 表示未投递。
     */
    @SuppressWarnings("unused")
    private ImMessageDTO sendNotificationFallback(ImMessageDTO message, Throwable t) {
        log.error("IM 通知发送降级触发 bizRefId={} cause={}", message.getBizRefId(), t.getMessage());
        return null;
    }
}
