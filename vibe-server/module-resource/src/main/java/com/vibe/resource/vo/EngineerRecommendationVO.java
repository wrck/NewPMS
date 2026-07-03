package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 工程师推荐结果（含匹配度评分）
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师推荐结果")
public class EngineerRecommendationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "状态 ACTIVE/RESIGNED")
    private String status;

    @Schema(description = "技能列表")
    private List<EngineerSkillVO> skills;

    @Schema(description = "综合匹配度评分（0-100）")
    private BigDecimal score;

    @Schema(description = "技能匹配度评分（0-100，权重 40%）")
    private BigDecimal skillScore;

    @Schema(description = "区域就近评分（0-100，权重 30%）")
    private BigDecimal regionScore;

    @Schema(description = "当前负荷评分（0-100，权重 30%；负荷越低分越高）")
    private BigDecimal workloadScore;

    @Schema(description = "时段内已分配任务数")
    private Integer currentWorkload;

    @Schema(description = "是否有时段冲突")
    private Boolean hasConflict;

    @Schema(description = "推荐理由")
    private String reason;
}
