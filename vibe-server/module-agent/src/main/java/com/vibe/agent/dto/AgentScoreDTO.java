package com.vibe.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理商评分 DTO
 *
 * <p>PM 对代理商多维度打分：及时性/质量/沟通/问题处理，每项 0-100。
 * 综合评分通过加权平均（30%/30%/20%/20%）计算后更新 agent_company.overall_score。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "代理商评分")
public class AgentScoreDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "代理商公司ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "代理商公司ID不能为空")
    private Long agentCompanyId;

    @Schema(description = "关联转包任务ID")
    private Long outsourceTaskId;

    @Schema(description = "交付及时性评分（0-100）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "及时性评分不能为空")
    @DecimalMin(value = "0", message = "评分不能小于0")
    @DecimalMax(value = "100", message = "评分不能大于100")
    private BigDecimal scoreTimeliness;

    @Schema(description = "交付质量评分（0-100）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "质量评分不能为空")
    @DecimalMin(value = "0", message = "评分不能小于0")
    @DecimalMax(value = "100", message = "评分不能大于100")
    private BigDecimal scoreQuality;

    @Schema(description = "沟通协作评分（0-100）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "沟通评分不能为空")
    @DecimalMin(value = "0", message = "评分不能小于0")
    @DecimalMax(value = "100", message = "评分不能大于100")
    private BigDecimal scoreCommunication;

    @Schema(description = "问题处理评分（0-100）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "问题处理评分不能为空")
    @DecimalMin(value = "0", message = "评分不能小于0")
    @DecimalMax(value = "100", message = "评分不能大于100")
    private BigDecimal scoreIssue;

    @Schema(description = "评语")
    private String remark;
}
