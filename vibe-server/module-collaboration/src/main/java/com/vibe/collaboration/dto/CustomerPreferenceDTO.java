package com.vibe.collaboration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户偏好新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "客户偏好新增/编辑")
public class CustomerPreferenceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "偏好ID（编辑时必填）")
    private Long id;

    @Schema(description = "客户ID（由路径参数填充，可省略）")
    private Long customerId;

    @Schema(description = "偏好键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "偏好键不能为空")
    @Size(max = 128, message = "偏好键长度不能超过128")
    private String preferenceKey;

    @Schema(description = "偏好值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "偏好值不能为空")
    @Size(max = 512, message = "偏好值长度不能超过512")
    private String preferenceValue;
}
