package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知模板新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "通知模板新增/编辑")
public class SysNoticeTemplateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID（编辑时必填）")
    private Long id;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码长度不能超过64")
    private String templateCode;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称长度不能超过128")
    private String templateName;

    @Schema(description = "标题模板")
    @Size(max = 255, message = "标题模板长度不能超过255")
    private String titleTemplate;

    @Schema(description = "内容模板（含变量占位符 ${var}）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;

    @Schema(description = "触达渠道（JSON 数组字符串，如 [\"FEISHU\",\"SITE\"]）")
    private String channels;

    @Schema(description = "接收人类型 ENGINEER/PM/AGENT/CUSTOMER/MANAGER")
    @Size(max = 32, message = "接收人类型长度不能超过32")
    private String recipientType;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
