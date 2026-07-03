package com.vibe.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 站内信实体（sys_notice）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
@Schema(description = "站内信")
public class SysNoticeEntity extends SysBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

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
