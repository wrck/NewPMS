package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 负荷热力图数据项
 *
 * <p>表示某工程师在某日的任务数（颜色深浅来源）。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "负荷热力图数据项")
public class WorkloadHeatmapVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "日期")
    private LocalDate date;

    @Schema(description = "当日任务数")
    private Integer taskCount;

    @Schema(description = "负荷等级 LOW/MEDIUM/HIGH/OVERLOAD")
    private String loadLevel;
}
