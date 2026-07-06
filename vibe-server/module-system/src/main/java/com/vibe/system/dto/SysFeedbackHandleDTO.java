package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 反馈处理 DTO（管理员使用）
 *
 * @author vibe
 */
@Data
@Schema(description = "反馈处理")
public class SysFeedbackHandleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标状态 PROCESSING/RESOLVED/CLOSED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标状态不能为空")
    @Pattern(regexp = "PROCESSING|RESOLVED|CLOSED", message = "目标状态只能为 PROCESSING/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "处理备注")
    @Size(max = 1000, message = "处理备注长度不能超过1000")
    private String handleNote;
}
