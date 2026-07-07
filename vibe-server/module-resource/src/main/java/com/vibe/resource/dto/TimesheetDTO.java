package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工时填报 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工时填报")
public class TimesheetDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工时ID（编辑时必填）")
    private Long id;

    @Schema(description = "工程师ID（PM 代填时必填，工程师自填可省略）")
    private Long engineerId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "工作日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工作日期不能为空")
    private LocalDate workDate;

    @Schema(description = "工作时长（小时）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工作时长不能为空")
    @DecimalMin(value = "0", message = "工作时长不能为负")
    private BigDecimal hours;

    @Schema(description = "加班时长（小时）")
    @DecimalMin(value = "0", message = "加班时长不能为负")
    private BigDecimal overtimeHours;

    @Schema(description = "工作类型 NORMAL/OVERTIME/BUSINESS_TRIP/WEEKEND")
    private String workType;

    @Size(max = 512, message = "工作内容说明长度不能超过512")
    @Schema(description = "工作内容说明")
    private String description;
}
