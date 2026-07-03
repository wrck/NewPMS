package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 冲突检测结果
 *
 * @author vibe
 */
@Data
@Schema(description = "冲突检测结果")
public class ConflictDetectVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否冲突")
    private Boolean conflict;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "待检测开始时间")
    private LocalDateTime startTime;

    @Schema(description = "待检测结束时间")
    private LocalDateTime endTime;

    @Schema(description = "冲突排期列表（不冲突时为空）")
    private List<EngineerScheduleVO> conflictSchedules;
}
