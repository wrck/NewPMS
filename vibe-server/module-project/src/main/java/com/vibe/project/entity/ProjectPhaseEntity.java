package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 项目阶段实体（project_phase）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_phase")
@Schema(description = "项目阶段")
public class ProjectPhaseEntity extends ProjectBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "阶段编码 SURVEY/DESIGN/DELIVER/INSTALL/DEBUG/ACCEPT")
    private String phaseCode;

    @Schema(description = "阶段名称")
    private String phaseName;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态 NOT_STARTED/IN_PROGRESS/COMPLETED")
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
