package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 异常问题处理 DTO（状态流转）
 *
 * @author vibe
 */
@Data
@Schema(description = "异常问题处理")
public class WorkOrderIssueProcessDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标状态 PROCESSING/RESOLVED/CLOSED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标状态不能为空")
    private String status;

    @Schema(description = "处理备注")
    private String remark;
}
