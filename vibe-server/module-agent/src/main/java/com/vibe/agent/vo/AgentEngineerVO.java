package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理商工程师视图对象
 *
 * <p>skills / certifications 为数据库 JSON 列，VO 中以 String 接收原始 JSON 文本，
 * 通过 {@link JsonRawValue} 注解让 Jackson 在序列化时直接输出原始 JSON（数组形式）。</p>
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "代理商工程师信息")
public class AgentEngineerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long id;

    @Schema(description = "所属代理商ID")
    private Long agentCompanyId;

    @Schema(description = "所属代理商名称")
    private String agentCompanyName;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    /** 技能标签（JSON 数组原始字符串，序列化时直接输出为 JSON 数组） */
    @JsonRawValue
    @Schema(description = "技能标签 JSON")
    private String skills;

    /** 认证资质（JSON 数组原始字符串） */
    @JsonRawValue
    @Schema(description = "认证资质 JSON")
    private String certifications;

    @Schema(description = "状态 ACTIVE/DISABLED")
    private String status;

    @Schema(description = "质量评分")
    private BigDecimal qualityScore;

    @Schema(description = "任务数（统计）")
    private Integer taskCount;

    @Schema(description = "加入时间")
    private LocalDateTime joinedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
