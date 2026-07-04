package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 月度项目趋势 VO
 *
 * <p>字段名对齐前端 {@code ProjectTrend} 接口定义。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "月度项目趋势")
public class ProjectTrendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 月份（yyyy-MM） */
    @Schema(description = "月份（yyyy-MM）")
    private String month;

    /** 当月新增项目数 */
    @Schema(description = "当月新增项目数")
    private Long newCount;

    /** 当月结项项目数 */
    @Schema(description = "当月结项项目数")
    private Long closedCount;

    /** 当月在建项目数 */
    @Schema(description = "当月在建项目数")
    private Long ongoingCount;

    public ProjectTrendVO() {
    }

    public ProjectTrendVO(String month, Long newCount, Long closedCount, Long ongoingCount) {
        this.month = month;
        this.newCount = newCount;
        this.closedCount = closedCount;
        this.ongoingCount = ongoingCount;
    }
}
