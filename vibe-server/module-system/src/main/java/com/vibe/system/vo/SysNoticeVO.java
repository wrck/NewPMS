package com.vibe.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内信视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "站内信")
public class SysNoticeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "通知标题")
    private String noticeTitle;

    @Schema(description = "通知类型 1-通知 2-消息")
    private Integer noticeType;

    @Schema(description = "通知内容")
    private String noticeContent;

    @Schema(description = "接收人ID")
    private Long recipientId;

    @Schema(description = "已读状态 0-未读 1-已读")
    private Integer readStatus;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;
}
