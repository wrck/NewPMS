package com.vibe.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.agent.constant.AgentConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 代理商公司实体（agent_company）
 *
 * <p>对应 schema.sql 中的 agent_company 表，存储代理商企业档案、合作区域、产品线、
 * 合作状态与综合评分。company_name / company_code 唯一。</p>
 *
 * <p>service_regions / product_lines 为 JSON 列，MyBatis-Plus 以字符串形式读写，
 * 业务层通过 Hutool JSONUtil 解析为 List&lt;String&gt;。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_company")
@Schema(description = "代理商公司")
public class AgentCompanyEntity extends AgentBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "公司编码")
    private String companyCode;

    @Schema(description = "资质等级")
    private String qualification;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    /** 服务区域列表（JSON 数组字符串） */
    @Schema(description = "服务区域列表 JSON")
    private String serviceRegions;

    /** 服务产品线列表（JSON 数组字符串） */
    @Schema(description = "服务产品线列表 JSON")
    private String productLines;

    /** 状态 ACTIVE/SUSPENDED/TERMINATED */
    @Schema(description = "合作状态")
    private String status;

    /** 综合评分（0-100） */
    @Schema(description = "综合评分")
    private BigDecimal overallScore;

    /** 合作开始日期 */
    @Schema(description = "合作开始日期")
    private LocalDate cooperationStart;

    /**
     * 判断是否为活跃状态。
     */
    public boolean isActive() {
        return AgentConstant.COMPANY_STATUS_ACTIVE.equals(this.status);
    }
}
