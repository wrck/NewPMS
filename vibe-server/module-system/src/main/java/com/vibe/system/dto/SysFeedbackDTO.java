package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 反馈提交 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "反馈提交")
public class SysFeedbackDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "反馈类型 BUG/SUGGESTION/QUESTION", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "反馈类型不能为空")
    @Pattern(regexp = "BUG|SUGGESTION|QUESTION", message = "反馈类型只能为 BUG/SUGGESTION/QUESTION")
    private String type;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @Schema(description = "内容描述")
    @Size(max = 2000, message = "内容长度不能超过2000")
    private String content;

    @Schema(description = "截图 URL（多个用逗号分隔）")
    @Size(max = 1000, message = "截图URL过长")
    private String screenshotUrl;

    @Schema(description = "联系方式")
    @Size(max = 100, message = "联系方式长度不能超过100")
    private String contact;
}
