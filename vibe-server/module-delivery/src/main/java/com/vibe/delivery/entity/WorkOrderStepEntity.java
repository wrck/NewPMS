package com.vibe.delivery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 工单施工步骤实体（work_order_step 表）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_step")
@Schema(description = "工单施工步骤")
public class WorkOrderStepEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

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
}
