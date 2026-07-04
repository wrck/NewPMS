package com.vibe.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成本归集实体（finance_cost 表，含 @Version 乐观锁）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_cost")
@Schema(description = "成本归集")
public class FinanceCostEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "成本类型 LABOR/TRAVEL/AGENT/OTHER")
    private String costType;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "发生日期")
    private LocalDate costDate;

    @Schema(description = "关联业务类型 TIMESHEET/BUSINESS_TRIP/OUTSOURCE_TASK/MANUAL")
    private String refType;

    @Schema(description = "关联业务ID")
    private Long refId;

    @Schema(description = "费用说明")
    private String description;
}
