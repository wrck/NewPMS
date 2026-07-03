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
    private Integer stepNo;

    @Schema(description = "步骤名称")
    private String stepName;

    @Schema(description = "状态 WAITING/COMPLETED/SKIPPED")
    private String status;

    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "耗时（秒）")
    private Integer duration;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
