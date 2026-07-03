package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代理商公司视图对象
 *
 * <p>service_regions / product_lines 为数据库 JSON 列，VO 中以 String 接收原始 JSON 文本，
 * 通过 {@link JsonRawValue} 注解让 Jackson 在序列化时直接输出原始 JSON（数组形式），
 * 无需在 Service 层手动解析为 List。</p>
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "代理商公司信息")
public class AgentCompanyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "公司ID")
    private Long id;

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

    /** 服务区域列表（JSON 数组原始字符串，序列化时直接输出为 JSON 数组） */
    @JsonRawValue
    @Schema(description = "服务区域列表 JSON")
    private String serviceRegions;

    /** 服务产品线列表（JSON 数组原始字符串） */
    @JsonRawValue
    @Schema(description = "服务产品线列表 JSON")
    private String productLines;

    @Schema(description = "合作状态 ACTIVE/SUSPENDED/TERMINATED")
    private String status;

    @Schema(description = "综合评分")
    private java.math.BigDecimal overallScore;

    @Schema(description = "合作开始日期")
    private java.time.LocalDate cooperationStart;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
