package com.vibe.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目报表查询 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目报表查询")
public class ProjectReportQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "PM 用户ID")
    private Long pmId;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "区域")
    private String region;
}
