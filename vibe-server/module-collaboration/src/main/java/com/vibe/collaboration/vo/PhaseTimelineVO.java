package com.vibe.collaboration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目阶段时间线 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目阶段时间线")
public class PhaseTimelineVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "阶段ID")
    private Long phaseId;

    @Schema(description = "阶段编码 SURVEY/DESIGN/DELIVER/INSTALL/DEBUG/ACCEPT")
    private String phaseCode;

    @Schema(description = "阶段名称")
    private String phaseName;

    @Schema(description = "阶段状态 NOT_STARTED/IN_PROGRESS/COMPLETED")
    private String status;

    @Schema(description = "计划开始")
    private LocalDate plannedStart;

    @Schema(description = "计划结束")
    private LocalDate plannedEnd;

    @Schema(description = "实际开始")
    private LocalDate actualStart;

    @Schema(description = "实际结束")
    private LocalDate actualEnd;

    @Schema(description = "交付物清单（JSON 字符串）")
    private String deliverables;
}
