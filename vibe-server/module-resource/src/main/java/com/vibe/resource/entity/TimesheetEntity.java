package com.vibe.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时实体（timesheet，含乐观锁）
 *
 * <p>工程师按项目/任务填报工时，PM 审批。状态流转：SUBMITTED → APPROVED / REJECTED。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("timesheet")
@Schema(description = "工时")
public class TimesheetEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "工作日期")
    private LocalDate workDate;

    @Schema(description = "工作时长（小时）")
    private BigDecimal hours;

    @Schema(description = "加班时长（小时）")
    private BigDecimal overtimeHours;

    @Schema(description = "出差天数")
    private Integer travelDays;

    @Schema(description = "工作内容说明")
    private String description;

    @Schema(description = "状态 SUBMITTED/APPROVED/REJECTED")
    private String status;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;
}
