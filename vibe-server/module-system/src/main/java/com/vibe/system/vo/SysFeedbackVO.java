package com.vibe.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 反馈视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "反馈")
public class SysFeedbackVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "反馈类型 BUG/SUGGESTION/QUESTION")
    private String type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容描述")
    private String content;

    @Schema(description = "截图 URL")
    private String screenshotUrl;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "提交人 ID")
    private Long submitterId;

    @Schema(description = "提交人姓名（关联 sys_user.real_name）")
    private String submitterName;

    @Schema(description = "状态 PENDING/PROCESSING/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "处理人 ID")
    private Long handlerId;

    @Schema(description = "处理人姓名")
    private String handlerName;

    @Schema(description = "处理备注")
    private String handleNote;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
