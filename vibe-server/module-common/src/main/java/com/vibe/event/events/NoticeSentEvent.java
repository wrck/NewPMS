package com.vibe.event.events;

import com.vibe.event.DomainEvent;
import com.vibe.event.DomainEventConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 通知发送事件
 *
 * <p>触发时机：通知引擎成功发送一条通知后发布（用于通知审计/统计/重试追踪）。
 * 下游消费者：BI 统计（通知送达率）、ES 同步、通知日志归档。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通知发送事件")
public class NoticeSentEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID")
    private Long noticeId;

    @Schema(description = "通知模板编码")
    private String templateCode;

    @Schema(description = "接收人ID")
    private Long recipientId;

    @Schema(description = "接收人类型：INTERNAL/AGENT/CUSTOMER")
    private String recipientType;

    @Schema(description = "通知渠道：FEISHU/DINGTALK/SITE/SMS/EMAIL")
    private String channel;

    @Schema(description = "发送状态：SUCCESS/FAILED")
    private String sendStatus;

    public NoticeSentEvent() {
        super(DomainEventConstant.EVENT_NOTICE_SENT, null);
    }

    public NoticeSentEvent(Long noticeId, String templateCode, Long recipientId,
                           String recipientType, String channel, String sendStatus) {
        super(DomainEventConstant.EVENT_NOTICE_SENT, String.valueOf(noticeId));
        this.noticeId = noticeId;
        this.templateCode = templateCode;
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.channel = channel;
        this.sendStatus = sendStatus;
    }
}
