package com.vibe.acceptance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 验收检查项实体（acceptance_standard_item 表）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acceptance_standard_item")
@Schema(description = "验收检查项")
public class AcceptanceStandardItemEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属验收标准ID")
    private Long standardId;

    @Schema(description = "检查项名称")
    private String name;

    @Schema(description = "检查要求")
    private String requirement;

    @Schema(description = "测试方法")
    private String testMethod;

    @Schema(description = "权重")
    private BigDecimal weight;

    @Schema(description = "排序")
    private Integer sortOrder;
}
