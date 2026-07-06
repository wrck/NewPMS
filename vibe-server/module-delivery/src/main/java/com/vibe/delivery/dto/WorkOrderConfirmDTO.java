package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单完成确认 DTO（PM 确认时使用）
 *
 * @author vibe
 */
@Data
@Schema(description = "工单完成确认")
public class WorkOrderConfirmDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否通过", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean approved;

    @Schema(description = "评分（0-5）")
    @Min(value = 0, message = "评分不能小于0")
    @Max(value = 5, message = "评分不能大于5")
    private Integer rating;

    @Schema(description = "确认备注")
    @Size(max = 500, message = "确认备注长度不能超过500")
    private String remark;
}
