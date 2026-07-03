package com.vibe.resource.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 排期查询 DTO（日历视图/负荷热力图/冲突检测共用）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "排期查询")
public class EngineerScheduleQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "排期类型 TASK/LEAVE/TRAINING/MEETING")
    private String scheduleType;

    @Schema(description = "查询开始时间")
    private LocalDateTime startTime;

    @Schema(description = "查询结束时间")
    private LocalDateTime endTime;

    @Schema(description = "区域（用于负荷热力图按区域过滤）")
    private String region;
}
