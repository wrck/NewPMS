package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "客户新增/编辑")
public class CustomerDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户ID（编辑时必填）")
    private Long id;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 128, message = "客户名称长度不能超过128")
    private String customerName;

    @Schema(description = "客户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "客户编码不能为空")
    @Size(max = 64, message = "客户编码长度不能超过64")
    private String customerCode;

    @Schema(description = "联系人")
    @Size(max = 64, message = "联系人长度不能超过64")
    private String contactName;

    @Schema(description = "联系电话")
    @Size(max = 32, message = "联系电话长度不能超过32")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    @Size(max = 128, message = "联系邮箱长度不能超过128")
    private String contactEmail;

    @Schema(description = "详细地址")
    @Size(max = 255, message = "详细地址长度不能超过255")
    private String address;

    @Schema(description = "区域")
    @Size(max = 32, message = "区域长度不能超过32")
    private String region;

    @Schema(description = "行业")
    @Size(max = 64, message = "行业长度不能超过64")
    private String industry;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
