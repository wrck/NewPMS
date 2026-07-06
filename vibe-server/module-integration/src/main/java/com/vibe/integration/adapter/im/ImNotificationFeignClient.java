package com.vibe.integration.adapter.im;

import com.vibe.integration.adapter.im.dto.ImMessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * IM 通知 Feign 客户端
 *
 * <p>对接 IM 系统（飞书/钉钉/企业微信）{@code ${integration.im.url}} 消息发送接口，
 * 转发本系统内部通知到外部 IM 平台。</p>
 *
 * @author vibe
 */
@FeignClient(name = "im-notification", url = "${integration.im.url}")
public interface ImNotificationFeignClient {

    /**
     * 发送 IM 通知消息。
     *
     * @param message IM 消息体
     * @return 发送结果（含 IM 侧 msgId）
     */
    @PostMapping("/messages/send")
    ImMessageDTO sendNotification(@RequestBody ImMessageDTO message);

    /**
     * 批量发送 IM 通知消息。
     *
     * @param messages 消息列表
     * @return 发送结果列表
     */
    @PostMapping("/messages/batch-send")
    java.util.List<ImMessageDTO> batchSendNotification(@RequestBody java.util.List<ImMessageDTO> messages);
}
