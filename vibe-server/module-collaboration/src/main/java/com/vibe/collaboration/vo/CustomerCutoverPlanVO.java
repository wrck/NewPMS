package com.vibe.collaboration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户视角的割接方案 VO（脱敏）
 *
 * <p>仅暴露客户可见字段，不包含内部审批信息、申请用户ID等内部细节。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户割接方案视图")
public class CustomerCutoverPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "方案ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "方案名称")
    private String planName;

    @Schema(description = "割接日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate cutoverDate;

    @Schema(description = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "计划结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "影响范围")
    private String impactScope;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "方案状态（客户可见子集）", example = "PENDING_CUSTOMER_APPROVAL")
    private String status;

    @Schema(description = "客户签核结果 APPROVED/REJECTED/null")
    private String customerSignResult;

    @Schema(description = "客户签核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime customerSignTime;

    @Schema(description = "客户签核意见")
    private String customerSignRemark;

    @Schema(description = "步骤总数")
    private Integer stepCount;

    @Schema(description = "已完成步骤数")
    private Integer completedStepCount;

    @Schema(description = "步骤列表")
    private List<CustomerCutoverStepVO> steps;

    /**
     * 客户视角的割接步骤 VO（脱敏）
     */
    @Data
    @Schema(description = "客户割接步骤视图")
    public static class CustomerCutoverStepVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "步骤ID")
        private Long id;

        @Schema(description = "步骤序号")
        private Integer sortOrder;

        @Schema(description = "步骤名称")
        private String stepName;

        @Schema(description = "步骤描述")
        private String description;

        @Schema(description = "预计耗时(分钟)")
        private Integer estimatedDuration;

        @Schema(description = "负责人姓名")
        private String ownerName;

        @Schema(description = "步骤状态")
        private String status;

        @Schema(description = "实际开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime actualStartTime;

        @Schema(description = "实际结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime actualEndTime;

        @Schema(description = "实际耗时(分钟)")
        private Integer actualDuration;
    }
}
