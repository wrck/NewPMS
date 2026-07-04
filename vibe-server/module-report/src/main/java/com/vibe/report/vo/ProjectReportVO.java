package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 项目报表 VO
 *
 * <p>对齐前端 {@code report.ts -> getProjectReport} 返回结构。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "项目报表")
public class ProjectReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "汇总指标")
    private Summary summary;

    @Schema(description = "按状态分布")
    private List<StatusStat> byStatus;

    @Schema(description = "按产品线分布")
    private List<ProductLineStat> byProductLine;

    @Schema(description = "按区域分布")
    private List<RegionStat> byRegion;

    @Schema(description = "PM 业绩")
    private List<PmStat> byPm;

    @Schema(description = "项目明细")
    private List<ProjectDetail> detail;

    @Data
    @Schema(description = "项目汇总")
    public static class Summary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "项目总数") private Long total;
        @Schema(description = "已完成") private Long completed;
        @Schema(description = "进行中") private Long ongoing;
        @Schema(description = "超期") private Long overdue;
        @Schema(description = "平均进度百分比") private BigDecimal avgProgress;
    }

    @Data
    @Schema(description = "状态分布")
    public static class StatusStat implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "状态码") private String status;
        @Schema(description = "状态名称") private String statusName;
        @Schema(description = "项目数") private Long count;
    }

    @Data
    @Schema(description = "产品线分布")
    public static class ProductLineStat implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "产品线") private String productLine;
        @Schema(description = "项目数") private Long count;
    }

    @Data
    @Schema(description = "区域分布")
    public static class RegionStat implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "区域") private String region;
        @Schema(description = "项目数") private Long count;
    }

    @Data
    @Schema(description = "PM 业绩")
    public static class PmStat implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "PM 用户ID") private Long pmId;
        @Schema(description = "PM 姓名") private String pmName;
        @Schema(description = "负责项目数") private Long total;
        @Schema(description = "已完成") private Long completed;
        @Schema(description = "超期") private Long overdue;
    }

    @Data
    @Schema(description = "项目明细")
    public static class ProjectDetail implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "项目ID") private Long id;
        @Schema(description = "项目编号") private String projectCode;
        @Schema(description = "项目名称") private String projectName;
        @Schema(description = "状态") private String status;
        @Schema(description = "进度百分比") private Integer progressPct;
        @Schema(description = "PM 姓名") private String pmName;
        @Schema(description = "计划开始") private LocalDate plannedStart;
        @Schema(description = "计划结束") private LocalDate plannedEnd;
        @Schema(description = "实际结束") private LocalDate actualEnd;
        @Schema(description = "是否超期") private Boolean overdue;
    }
}
