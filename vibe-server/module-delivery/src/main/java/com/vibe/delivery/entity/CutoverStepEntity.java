package com.vibe.delivery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 割接步骤实体（cutover_step 表，含 @Version 乐观锁）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cutover_step")
@Schema(description = "割接步骤")
public class CutoverStepEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属割接方案ID")
    private Long planId;

    @Schema(description = "步骤序号")
    private Integer sortOrder;

    @Schema(description = "步骤名称")
    private String stepName;

    @Schema(description = "详细操作说明")
    private String description;

    @Schema(description = "预估耗时（分钟）")
    private Integer estimatedDuration;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "回退方案")
    private String rollbackPlan;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "实际耗时（分钟）")
    private Integer actualDuration;

    @Schema(description = "执行备注")
    private String executionRemark;

    @Schema(description = "异常说明")
    private String exceptionRemark;
}
