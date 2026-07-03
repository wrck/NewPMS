package com.vibe.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.agent.constant.AgentConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 代理商工程师实体（agent_engineer）
 *
 * <p>对应 schema.sql 中的 agent_engineer 表，存储代理商工程师档案、技能标签、
 * 认证资质与质量评分。skills / certifications 为 JSON 列。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_engineer")
@Schema(description = "代理商工程师")
public class AgentEngineerEntity extends AgentBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属代理商ID */
    @Schema(description = "所属代理商ID")
    private Long agentCompanyId;

    /** 姓名 */
    @Schema(description = "姓名")
    private String name;

    /** 手机号（登录账号） */
    @Schema(description = "手机号")
    private String phone;

    /** 技能标签（JSON 数组字符串） */
    @Schema(description = "技能标签 JSON")
    private String skills;

    /** 认证资质（JSON 数组字符串） */
    @Schema(description = "认证资质 JSON")
    private String certifications;

    /** 状态 ACTIVE/DISABLED */
    @Schema(description = "状态")
    private String status;

    /** 质量评分 */
    @Schema(description = "质量评分")
    private BigDecimal qualityScore;

    /**
     * 判断是否为启用状态。
     */
    public boolean isActive() {
        return AgentConstant.ENGINEER_STATUS_ACTIVE.equals(this.status);
    }
}
