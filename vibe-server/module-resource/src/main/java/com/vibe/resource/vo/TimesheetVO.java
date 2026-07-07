package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "工时信息")
public class TimesheetVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工时ID")
    private Long id;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "工作日期")
    private LocalDate workDate;

    @Schema(description = "工作时长（小时）")
    private BigDecimal hours;

    @Schema(description = "加班时长（小时）")
    private BigDecimal overtimeHours;

    @Schema(description = "工作类型 NORMAL/OVERTIME/BUSINESS_TRIP/WEEKEND")
    private String workType;

    @Schema(description = "工作内容说明")
    private String description;

    @Schema(description = "状态 DRAFT/SUBMITTED/APPROVED/REJECTED")
    private String status;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批人姓名")
    private String approverName;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
