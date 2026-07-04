package com.vibe.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成本归集 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "成本归集")
public class FinanceCostVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "成本类型 LABOR/TRAVEL/AGENT/OTHER")
    private String costType;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "发生日期")
    private LocalDate costDate;

    @Schema(description = "关联业务类型")
    private String refType;

    @Schema(description = "关联业务ID")
    private Long refId;

    @Schema(description = "费用说明")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
