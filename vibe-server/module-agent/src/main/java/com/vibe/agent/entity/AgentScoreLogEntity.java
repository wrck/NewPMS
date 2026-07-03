package com.vibe.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 代理商评分记录实体（agent_score_log）
 *
 * <p>对应 schema.sql 中的 agent_score_log 表，存储 PM 对代理商的多维度评分。
 * 评分维度为独立列：score_timeliness（及时性）/ score_quality（质量）/
 * score_communication（沟通）/ score_issue（问题处理），综合评分通过加权平均
 * 计算后更新 agent_company.overall_score。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_score_log")
@Schema(description = "代理商评分记录")
public class AgentScoreLogEntity extends AgentBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 代理商公司ID */
    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    /** 关联转包任务ID */
    @Schema(description = "关联转包任务ID")
    private Long outsourceTaskId;

    /** 交付及时性评分（0-100） */
    @Schema(description = "交付及时性评分")
    private BigDecimal scoreTimeliness;

    /** 交付质量评分（0-100） */
    @Schema(description = "交付质量评分")
    private BigDecimal scoreQuality;

    /** 沟通协作评分（0-100） */
    @Schema(description = "沟通协作评分")
    private BigDecimal scoreCommunication;

    /** 问题处理评分（0-100） */
    @Schema(description = "问题处理评分")
    private BigDecimal scoreIssue;

    /** 评分人ID */
    @Schema(description = "评分人ID")
    private Long scorerId;

    /** 评语 */
    @Schema(description = "评语")
    private String remark;
}
