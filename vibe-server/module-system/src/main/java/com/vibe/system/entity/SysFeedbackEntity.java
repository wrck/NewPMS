package com.vibe.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 反馈与工单实体（sys_feedback）
 *
 * <p>用于收集用户在使用过程中提交的 Bug 报告、功能建议、咨询等反馈，
 * 管理员可在「系统管理 &gt; 反馈管理」中处理并通知提交人。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_feedback")
@Schema(description = "反馈与工单")
public class SysFeedbackEntity extends SysBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 反馈类型：BUG / SUGGESTION / QUESTION */
    @Schema(description = "反馈类型 BUG-缺陷 SUGGESTION-建议 QUESTION-咨询")
    private String type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容描述")
    private String content;

    @Schema(description = "截图 URL（多个用逗号分隔）")
    private String screenshotUrl;

    @Schema(description = "联系方式（手机号/邮箱/IM 账号）")
    private String contact;

    @Schema(description = "提交人 ID（取自当前登录用户）")
    private Long submitterId;

    /** 状态：PENDING 待处理 / PROCESSING 处理中 / RESOLVED 已解决 / CLOSED 已关闭 */
    @Schema(description = "状态 PENDING-待处理 PROCESSING-处理中 RESOLVED-已解决 CLOSED-已关闭")
    private String status;

    @Schema(description = "处理人 ID")
    private Long handlerId;

    @Schema(description = "处理备注")
    private String handleNote;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;
}
