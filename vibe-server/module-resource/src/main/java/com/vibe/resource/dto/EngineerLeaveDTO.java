package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 工程师请假 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师请假")
public class EngineerLeaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "请假ID（编辑时必填）")
    private Long id;

    @Schema(description = "工程师ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工程师ID不能为空")
    private Long engineerId;

    @Schema(description = "开始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "请假类型 ANNUAL/SICK/PERSONAL/OTHER")
    private String leaveType;

    @Size(max = 255, message = "请假原因长度不能超过255")
    @Schema(description = "请假原因")
    private String reason;

    @Schema(description = "状态 PENDING/APPROVED/REJECTED")
    private String status;
}
