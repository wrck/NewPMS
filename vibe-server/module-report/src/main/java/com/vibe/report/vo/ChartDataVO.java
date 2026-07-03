package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图表数据通用 VO
 *
 * <p>饼图/柱图：使用 {@link #name} + {@link #value} 字段。</p>
 * <p>折线图（项目趋势）：使用 {@link #month} + {@link #newCount} + {@link #completedCount} 字段。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "图表数据")
public class ChartDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 名称（饼图/柱图维度名，如状态名、阶段名） */
    @Schema(description = "名称（饼图/柱图维度名）")
    private String name;

    /** 数值（饼图/柱图计数值） */
    @Schema(description = "数值")
    private Long value;

    /** 月份（格式 yyyy-MM，折线图 X 轴） */
    @Schema(description = "月份（yyyy-MM）")
    private String month;

    /** 新增数（折线图：当月新增项目数） */
    @Schema(description = "新增数")
    private Long newCount;

    /** 完成数（折线图：当月完成项目数） */
    @Schema(description = "完成数")
    private Long completedCount;

    public ChartDataVO() {
    }

    /** 饼图/柱图构造方法 */
    public ChartDataVO(String name, Long value) {
        this.name = name;
        this.value = value;
    }

    /** 折线图构造方法 */
    public ChartDataVO(String month, Long newCount, Long completedCount) {
        this.month = month;
        this.newCount = newCount;
        this.completedCount = completedCount;
    }
}
