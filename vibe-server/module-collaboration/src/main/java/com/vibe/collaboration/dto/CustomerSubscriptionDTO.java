package com.vibe.collaboration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户订阅新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "客户订阅新增/编辑")
public class CustomerSubscriptionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订阅ID（编辑时必填）")
    private Long id;

    @Schema(description = "客户ID（由路径参数填充，可省略）")
    private Long customerId;

    @Schema(description = "事件类型 PROJECT_PROGRESS/CUTOVER_NOTICE/ACCEPTANCE_NOTICE/DOCUMENT_UPLOAD",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "事件类型不能为空")
    @Size(max = 64, message = "事件类型长度不能超过64")
    private String eventType;

    @Schema(description = "接收渠道（逗号分隔：SMS,EMAIL,IM）")
    @Size(max = 128, message = "渠道长度不能超过128")
    private String channels;

    @Schema(description = "订阅状态 SUBSCRIBED/UNSUBSCRIBED")
    private String status;
}
