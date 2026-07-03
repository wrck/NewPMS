package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 项目里程碑实体（project_milestone）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_milestone")
@Schema(description = "项目里程碑")
public class ProjectMilestoneEntity extends ProjectBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "里程碑名称")
    private String milestoneName;

    @Schema(description = "预计日期")
    private LocalDate plannedDate;

    @Schema(description = "实际日期")
    private LocalDate actualDate;

    @Schema(description = "交付物清单（JSON 字符串）")
    private String deliverables;

    @Schema(description = "状态 PENDING/REACHED/DELAYED")
    private String status;
}
