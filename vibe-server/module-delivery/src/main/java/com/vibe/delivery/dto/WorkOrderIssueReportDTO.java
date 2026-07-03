package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 异常问题上报 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "异常问题上报")
public class WorkOrderIssueReportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题类型（如 DEVICE_FAULT/CONFIG_ERROR/SITE_ISSUE/OTHER）")
    private String issueType;

    @Schema(description = "严重程度 MINOR/MAJOR/BLOCKING", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "严重程度不能为空")
    private String severity;

    @Schema(description = "问题描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题描述不能为空")
    @Size(max = 1000, message = "问题描述长度不能超过1000")
    private String description;

    @Schema(description = "问题照片地址列表（已上传到 MinIO 的 objectName）")
    private List<String> photos;

    @Schema(description = "备注")
    private String remark;
}
