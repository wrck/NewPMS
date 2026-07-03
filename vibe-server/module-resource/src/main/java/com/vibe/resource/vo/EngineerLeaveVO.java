package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工程师请假视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师请假")
public class EngineerLeaveVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "请假ID")
    private Long id;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "请假类型 ANNUAL/SICK/PERSONAL/OTHER")
    private String leaveType;

    @Schema(description = "请假原因")
    private String reason;

    @Schema(description = "状态 PENDING/APPROVED/REJECTED")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
