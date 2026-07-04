package com.vibe.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 割接步骤 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "割接步骤")
public class CutoverStepVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

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
