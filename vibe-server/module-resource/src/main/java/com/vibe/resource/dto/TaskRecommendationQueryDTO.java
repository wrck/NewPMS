package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能推荐查询 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "智能推荐工程师查询")
public class TaskRecommendationQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目任务ID不能为空")
    private Long taskId;

    @Schema(description = "所需技能标签列表（路由/交换/无线/安全/数据中心/布线）")
    private List<String> requiredSkills;

    @Schema(description = "任务区域（用于区域就近评分）")
    private String region;

    @Schema(description = "排期开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "排期结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "返回推荐数量（默认 10）")
    private Integer limit = 10;
}
