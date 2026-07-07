package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 工单创建 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工单创建")
public class WorkOrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工单名称不能为空")
    @Size(max = 128, message = "工单名称长度不能超过128")
    private String workOrderName;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Schema(description = "关联项目任务ID")
    private Long taskId;

    @Schema(description = "执行工程师ID")
    private Long engineerId;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商工程师ID")
    private Long agentEngineerId;

    @Schema(description = "执行模式 SELF/AGENT")
    private String executeMode;

    @Schema(description = "优先级 LOW/MEDIUM/HIGH/URGENT")
    private String priority;

    @Schema(description = "站点信息（JSON 字符串：siteName/address/contact/phone）")
    private String siteInfo;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStart;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEnd;

    @Schema(description = "工单描述")
    private String description;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "标准施工步骤列表（含 stepName/description/estimatedMinutes）")
    private List<StandardStep> standardSteps;

    /**
     * 标准施工步骤。
     */
    @Data
    @Schema(description = "标准施工步骤")
    public static class StandardStep implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "步骤名称")
        private String stepName;

        @Schema(description = "步骤描述")
        private String description;

        @Schema(description = "预计耗时（分钟）")
        private Integer estimatedMinutes;
    }
}
