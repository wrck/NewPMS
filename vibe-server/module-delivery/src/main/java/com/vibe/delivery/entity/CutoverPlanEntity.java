package com.vibe.delivery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 割接方案实体（cutover_plan 表，含 @Version 乐观锁）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cutover_plan")
@Schema(description = "割接方案")
public class CutoverPlanEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "割接方案名称")
    private String planName;

    @Schema(description = "割接日期")
    private LocalDate cutoverDate;

    @Schema(description = "计划开始时间")
    private LocalDateTime startTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime endTime;

    @Schema(description = "影响范围说明")
    private String impactScope;

    @Schema(description = "应急联系人")
    private String emergencyContact;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "编制人ID（PM）")
    private Long applyUserId;

    @Schema(description = "编制时间")
    private LocalDateTime applyTime;

    @Schema(description = "内部审批人ID")
    private Long approvalUserId;

    @Schema(description = "内部审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "内部审批意见")
    private String approvalRemark;

    @Schema(description = "客户审批链接token")
    private String customerSignLink;

    @Schema(description = "客户签核人姓名")
    private String customerSignUser;

    @Schema(description = "客户签核时间")
    private LocalDateTime customerSignTime;

    @Schema(description = "客户签核结果 APPROVED/REJECTED")
    private String customerSignResult;

    @Schema(description = "客户审批意见")
    private String customerSignRemark;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "割接总结")
    private String summary;

    @Schema(description = "问题与改进")
    private String problemImprovement;

    @Schema(description = "备注")
    private String remark;
}
