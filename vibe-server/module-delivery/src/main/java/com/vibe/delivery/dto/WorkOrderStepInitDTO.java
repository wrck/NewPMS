package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 工单步骤批量初始化 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工单步骤初始化")
public class WorkOrderStepInitDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "步骤名称列表（按序号排序）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤名称列表不能为空")
    private List<@NotBlank String> stepNames;
}
