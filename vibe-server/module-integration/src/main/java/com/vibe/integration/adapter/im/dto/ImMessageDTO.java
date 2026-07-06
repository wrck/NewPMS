package com.vibe.integration.adapter.im.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * IM 系统消息 DTO
 *
 * <p>用于 {@code ImNotificationFeignClient} 转发内部通知到 IM 系统（飞书/钉钉/企业微信）。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "IM 系统消息")
public class ImMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息 ID（IM 侧生成的唯一标识，发送时为空）")
    private String msgId;

    @Schema(description = "IM 平台类型（FEISHU/DINGTALK/WECOM）")
    private String platform;

    @Schema(description = "接收人 ID 列表（IM 侧用户 ID 或 open_id）")
    private List<String> receiverIds;

    @Schema(description = "接收群 ID（如果群消息则填充，与 receiverIds 互斥）")
    private String chatId;

    @Schema(description = "消息类型（TEXT/MARKDOWN/CARD/IMAGE）")
    private String msgType;

    @Schema(description = "消息内容（TEXT 类型为纯文本，MARKDOWN 类型为 markdown 文本）")
    private String content;

    @Schema(description = "消息标题（MARKDOWN/CARD 类型使用）")
    private String title;

    @Schema(description = "卡片模板 ID（CARD 类型使用）")
    private String cardTemplateId;

    @Schema(description = "业务关联 ID（用于幂等去重）")
    private String bizRefId;

    @Schema(description = "发送方应用标识")
    private String fromApp;

    @Schema(description = "是否@全部（仅群消息生效）")
    private Boolean atAll;

    @Schema(description = "@人 ID 列表（仅群消息生效）")
    private List<String> atUserIds;
}
