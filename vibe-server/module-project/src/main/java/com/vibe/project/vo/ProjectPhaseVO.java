package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目阶段视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目阶段")
public class ProjectPhaseVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "阶段ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "阶段编码")
    private String phaseCode;

    @Schema(description = "阶段名称")
    private String phaseName;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "计划开始")
    private LocalDate plannedStart;

    @Schema(description = "计划结束")
    private LocalDate plannedEnd;

    @Schema(description = "实际开始")
    private LocalDate actualStart;

    @Schema(description = "实际结束")
    private LocalDate actualEnd;

    @Schema(description = "交付物清单")
    private String deliverables;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
