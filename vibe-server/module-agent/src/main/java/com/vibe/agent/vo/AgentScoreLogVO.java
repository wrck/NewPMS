package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理商评分记录视图对象
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "代理商评分记录")
public class AgentScoreLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评分记录ID")
    private Long id;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "关联转包任务ID")
    private Long taskId;

    @Schema(description = "交付及时性评分")
    private BigDecimal timeliness;

    @Schema(description = "交付质量评分")
    private BigDecimal quality;

    @Schema(description = "沟通协作评分")
    private BigDecimal communication;

    @Schema(description = "问题处理评分")
    private BigDecimal issueRate;

    @Schema(description = "综合评分（加权平均）")
    private BigDecimal overallScore;

    @Schema(description = "评分人ID")
    private Long scorerId;

    @Schema(description = "评分人姓名")
    private String scorerName;

    @Schema(description = "评语")
    private String comment;

    @Schema(description = "评分时间")
    private LocalDateTime scoredAt;

    @Schema(description = "代理商公司名称")
    private String agentCompanyName;

    @Schema(description = "项目任务名称")
    private String taskName;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
