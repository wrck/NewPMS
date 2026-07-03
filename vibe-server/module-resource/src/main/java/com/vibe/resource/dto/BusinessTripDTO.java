package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出差申请 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "出差申请")
public class BusinessTripDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "出差ID（编辑时必填）")
    private Long id;

    @Schema(description = "工程师ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工程师ID不能为空")
    private Long engineerId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联任务ID")
    private Long taskId;

    @Size(max = 128, message = "出发地长度不能超过128")
    @Schema(description = "出发地")
    private String origin;

    @Schema(description = "目的地", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 128, message = "目的地长度不能超过128")
    private String destination;

    @Schema(description = "出差开始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "出差开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "出差结束日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "出差结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "交通方式 PLANE/TRAIN/CAR/OTHER")
    private String transportMode;

    @Size(max = 255, message = "住宿信息长度不能超过255")
    @Schema(description = "住宿信息")
    private String accommodation;

    @Schema(description = "预估费用")
    private BigDecimal estimatedCost;

    @Size(max = 512, message = "出差事由长度不能超过512")
    @Schema(description = "出差事由")
    private String reason;

    @Size(max = 255, message = "备注长度不能超过255")
    @Schema(description = "备注")
    private String remark;
}
