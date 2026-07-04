package com.vibe.collaboration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户消息 VO
 *
 * <p>展示客户收到的通知消息（项目进度更新/割接提醒/验收通知等）。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户消息")
public class CustomerMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "消息类型 PROJECT_PROGRESS/CUTOVER_NOTICE/ACCEPTANCE_NOTICE/DOCUMENT_UPLOAD")
    private String messageType;

    @Schema(description = "业务ID（项目ID/方案ID/任务ID）")
    private Long businessId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "是否已读 0-未读 1-已读")
    private Integer isRead;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
