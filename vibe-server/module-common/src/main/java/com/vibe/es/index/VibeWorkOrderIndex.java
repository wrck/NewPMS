package com.vibe.es.index;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单索引 POJO（Elasticsearch 文档）
 *
 * <p>对应 ES 索引 {@code vibe_work_order}，覆盖工单列表查询、工程师工单统计、状态聚合。</p>
 *
 * <p>字段对应关系（MySQL → ES）：</p>
 * <ul>
 *   <li>id ← work_order.id</li>
 *   <li>projectId ← work_order.project_id</li>
 *   <li>engineerId ← work_order.engineer_id</li>
 *   <li>status ← work_order.status</li>
 *   <li>plannedStart ← project_task.planned_start（关联查询）</li>
 *   <li>plannedEnd ← project_task.planned_end（关联查询）</li>
 *   <li>actualEnd ← work_order.checkout_time</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "工单索引文档")
public class VibeWorkOrderIndex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工单ID")
    private Long id;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "工程师姓名")
    private String engineerName;

    @Schema(description = "工单状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStart;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEnd;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "实际结束时间（签退时间）")
    private LocalDateTime actualEnd;
}
