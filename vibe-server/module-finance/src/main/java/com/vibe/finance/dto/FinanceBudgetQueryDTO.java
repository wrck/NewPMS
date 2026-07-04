package com.vibe.finance.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 预算查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "预算查询")
public class FinanceBudgetQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "预算年度")
    private Integer year;

    @Schema(description = "审批状态")
    private String approvalStatus;
}
