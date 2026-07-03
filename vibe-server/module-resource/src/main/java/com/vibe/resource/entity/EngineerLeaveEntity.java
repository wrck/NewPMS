package com.vibe.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 工程师请假实体（engineer_leave）
 *
 * <p>请假期间自动标记为不可分配时段（通过同步写入 engineer_schedule 的 LEAVE 时间块实现）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("engineer_leave")
@Schema(description = "工程师请假")
public class EngineerLeaveEntity extends ResourceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

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
}
