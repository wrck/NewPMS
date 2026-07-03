package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理商排名视图对象
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "代理商排名")
public class AgentRankingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "排名")
    private Integer rank;

    @Schema(description = "公司ID")
    private Long id;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "公司编码")
    private String companyCode;

    @Schema(description = "资质等级")
    private String qualification;

    @Schema(description = "综合评分")
    private BigDecimal overallScore;

    @Schema(description = "合作状态")
    private String status;
}
