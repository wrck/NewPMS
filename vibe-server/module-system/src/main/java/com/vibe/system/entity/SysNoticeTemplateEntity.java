package com.vibe.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 通知模板实体（sys_notice_template）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice_template")
@Schema(description = "通知模板")
public class SysNoticeTemplateEntity extends SysBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "标题模板（含变量占位符）")
    private String titleTemplate;

    @Schema(description = "内容模板（含变量占位符 ${var}）")
    private String contentTemplate;

    @Schema(description = "触达渠道 FEISHU/DINGTALK/SMS/EMAIL/SITE（JSON 数组字符串）")
    private String channels;

    @Schema(description = "接收人类型 ENGINEER/PM/AGENT/CUSTOMER/MANAGER")
    private String recipientType;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
