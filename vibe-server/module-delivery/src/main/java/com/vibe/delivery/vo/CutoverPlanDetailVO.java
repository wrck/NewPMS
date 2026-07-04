package com.vibe.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 割接方案详情 VO（含步骤、操作日志）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "割接方案详情")
public class CutoverPlanDetailVO extends CutoverPlanVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "影响范围说明")
    private String impactScope;

    @Schema(description = "应急联系人")
    private String emergencyContact;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "内部审批人ID")
    private Long approvalUserId;

    @Schema(description = "内部审批人姓名")
    private String approvalUserName;

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

    @Schema(description = "割接步骤列表")
    private List<CutoverStepVO> steps;

    @Schema(description = "操作日志列表")
    private List<CutoverExecutionLogVO> logs;
}
