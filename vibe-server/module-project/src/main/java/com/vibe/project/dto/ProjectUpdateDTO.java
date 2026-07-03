package com.vibe.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目编辑")
public class ProjectUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目ID不能为空")
    private Long id;

    @Schema(description = "项目名称")
    @Size(max = 128, message = "项目名称长度不能超过128")
    private String projectName;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "执行模式")
    private String executeMode;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "项目经理ID")
    private Long pmId;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "合同编号")
    @Size(max = 64, message = "合同编号长度不能超过64")
    private String contractNo;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEnd;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "备注")
    private String remark;
}
