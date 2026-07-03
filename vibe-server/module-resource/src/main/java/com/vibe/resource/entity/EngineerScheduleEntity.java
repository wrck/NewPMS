package com.vibe.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 工程师排期实体（engineer_schedule，含乐观锁）
 *
 * <p>记录工程师在某时间段被分配的任务/请假/培训/会议时间块。
 * 冲突检测基于该表：同一工程师同一时段存在多条 TASK 类型记录时判定为冲突。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("engineer_schedule")
@Schema(description = "工程师排期")
public class EngineerScheduleEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "关联任务ID（TASK 类型时必填）")
    private Long taskId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "排期类型 TASK/LEAVE/TRAINING/MEETING")
    private String scheduleType;

    @Schema(description = "备注")
    private String remark;
}
