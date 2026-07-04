package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 割接步骤 DTO（嵌入在 CutoverPlanCreateDTO 中传递）
 *
 * @author vibe
 */
@Data
@Schema(description = "割接步骤")
public class CutoverStepDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "步骤ID（更新时传入，新增时不传）")
    private Long id;

    @Schema(description = "步骤序号（从1开始）")
    private Integer sortOrder;

    @NotBlank(message = "步骤名称不能为空")
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
}
