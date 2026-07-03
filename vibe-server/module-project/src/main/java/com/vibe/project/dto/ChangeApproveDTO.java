package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 变更审批 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "变更审批")
public class ChangeApproveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "审批结果 APPROVED/REJECTED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "审批结果不能为空")
    private String approveResult;

    @Schema(description = "审批意见")
    private String opinion;
}
