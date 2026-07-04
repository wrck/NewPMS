package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 割接方案状态流转 DTO
 *
 * <p>用于内部审批、客户审批等动作。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "割接方案状态流转")
public class CutoverApprovalDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "割接方案ID不能为空")
    @Schema(description = "割接方案ID")
    private Long planId;

    @Schema(description = "审批结果 APPROVED/REJECTED")
    private String result;

    @Schema(description = "客户签核人姓名（客户审批时使用）")
    private String customerSignUser;

    @Schema(description = "审批意见")
    private String remark;
}
