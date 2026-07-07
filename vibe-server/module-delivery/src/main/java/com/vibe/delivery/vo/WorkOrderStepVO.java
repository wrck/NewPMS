package com.vibe.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单施工步骤视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "工单施工步骤")
public class WorkOrderStepVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "步骤ID")
    private Long id;

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "步骤序号")
    private Integer stepOrder;

    @Schema(description = "步骤名称")
    private String stepName;

    @Schema(description = "步骤描述")
    private String description;

    @Schema(description = "预计耗时（分钟）")
    private Integer estimatedMinutes;

    @Schema(description = "实际耗时（分钟）")
    private Integer actualMinutes;

    @Schema(description = "状态 PENDING/IN_PROGRESS/COMPLETED/SKIPPED")
    private String status;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
