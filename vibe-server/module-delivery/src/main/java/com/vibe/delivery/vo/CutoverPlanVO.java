package com.vibe.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 割接方案 VO（列表展示用）
 *
 * @author vibe
 */
@Data
@Schema(description = "割接方案")
public class CutoverPlanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "项目名称（联表查询）")
    private String projectName;

    @Schema(description = "方案名称")
    private String planName;

    @Schema(description = "割接日期")
    private LocalDate cutoverDate;

    @Schema(description = "计划开始时间")
    private LocalDateTime startTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime endTime;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "编制人ID")
    private Long applyUserId;

    @Schema(description = "编制人姓名")
    private String applyUserName;

    @Schema(description = "编制时间")
    private LocalDateTime applyTime;

    @Schema(description = "步骤总数")
    private Integer stepCount;

    @Schema(description = "已完成步骤数")
    private Integer completedStepCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
