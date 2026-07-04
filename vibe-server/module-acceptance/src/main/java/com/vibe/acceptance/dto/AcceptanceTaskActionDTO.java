package com.vibe.acceptance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 验收任务状态流转 DTO（申请/内部审核/客户签核等）
 *
 * @author vibe
 */
@Data
@Schema(description = "验收任务状态流转")
public class AcceptanceTaskActionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "验收任务ID不能为空")
    @Schema(description = "验收任务ID")
    private Long taskId;

    @Schema(description = "操作结果 PASS/REJECT/CONDITIONAL_PASS（客户签核时使用）")
    private String result;

    @Schema(description = "客户签核人姓名（客户签核时使用）")
    private String customerSignUser;

    @Schema(description = "备注")
    private String remark;
}
