package com.vibe.finance.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 代理商结算查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "代理商结算查询")
public class FinanceWorkloadQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "代理商ID")
    private Long agentCompanyId;

    @Schema(description = "对账周期")
    private String period;

    @Schema(description = "审批状态")
    private String approvalStatus;

    @Schema(description = "付款状态")
    private String paymentStatus;
}
