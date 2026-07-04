package com.vibe.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 资源报表查询 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "资源报表查询")
public class ResourceReportQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "组织ID")
    private Long orgId;
}
