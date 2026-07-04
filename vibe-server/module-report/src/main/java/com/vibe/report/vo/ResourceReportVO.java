package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 资源报表 VO
 *
 * <p>对齐前端 {@code report.ts -> getResourceReport} 返回结构。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "资源报表")
public class ResourceReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "汇总")
    private Summary summary;

    @Schema(description = "工程师维度统计")
    private List<EngineerStat> byEngineer;

    @Schema(description = "项目维度统计")
    private List<ProjectStat> byProject;

    @Data
    @Schema(description = "资源汇总")
    public static class Summary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "工程师总数") private Long totalEngineers;
        @Schema(description = "平均利用率（百分比）") private BigDecimal avgUtilization;
        @Schema(description = "总工时") private BigDecimal totalHours;
        @Schema(description = "加班工时") private BigDecimal overtimeHours;
    }

    @Data
    @Schema(description = "工程师统计")
    public static class EngineerStat implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "工程师ID") private Long engineerId;
        @Schema(description = "工程师姓名") private String engineerName;
        @Schema(description = "任务数") private Long taskCount;
        @Schema(description = "工时") private BigDecimal hours;
        @Schema(description = "加班工时") private BigDecimal overtimeHours;
        @Schema(description = "利用率（百分比）") private BigDecimal utilization;
        @Schema(description = "按时完成率（百分比）") private BigDecimal onTimeRate;
    }

    @Data
    @Schema(description = "项目统计")
    public static class ProjectStat implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @Schema(description = "项目ID") private Long projectId;
        @Schema(description = "项目名称") private String projectName;
        @Schema(description = "投入工时") private BigDecimal hours;
        @Schema(description = "投入工程师数") private Long engineerCount;
    }
}
