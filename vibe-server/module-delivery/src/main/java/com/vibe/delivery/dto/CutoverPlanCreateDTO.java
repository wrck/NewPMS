package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 割接方案创建/更新 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "割接方案创建/更新")
public class CutoverPlanCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "关联项目ID")
    private Long projectId;

    @NotBlank(message = "割接方案名称不能为空")
    @Schema(description = "割接方案名称")
    private String planName;

    @NotNull(message = "割接日期不能为空")
    @Schema(description = "割接日期")
    private LocalDate cutoverDate;

    @NotNull(message = "计划开始时间不能为空")
    @Schema(description = "计划开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "计划结束时间不能为空")
    @Schema(description = "计划结束时间")
    private LocalDateTime endTime;

    @Schema(description = "影响范围说明")
    private String impactScope;

    @Schema(description = "应急联系人")
    private String emergencyContact;

    @Schema(description = "备注")
    private String remark;

    @NotEmpty(message = "割接步骤不能为空")
    @Schema(description = "割接步骤列表（按 sort_order 升序）")
    private List<CutoverStepDTO> steps;
}
