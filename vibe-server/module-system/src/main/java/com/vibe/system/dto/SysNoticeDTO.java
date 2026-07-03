package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 站内信发送 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "站内信发送")
public class SysNoticeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "通知标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "通知标题不能为空")
    @Size(max = 128, message = "通知标题长度不能超过128")
    private String noticeTitle;

    @Schema(description = "通知类型 1-通知 2-消息")
    private Integer noticeType;

    @Schema(description = "通知内容")
    private String noticeContent;

    @Schema(description = "接收人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long recipientId;
}
