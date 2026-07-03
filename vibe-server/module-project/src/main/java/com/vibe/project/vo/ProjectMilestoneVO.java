package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目里程碑视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "项目里程碑")
public class ProjectMilestoneVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "里程碑ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "里程碑名称")
    private String milestoneName;

    @Schema(description = "预计日期")
    private LocalDate plannedDate;

    @Schema(description = "实际日期")
    private LocalDate actualDate;

    @Schema(description = "交付物清单")
    private String deliverables;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
