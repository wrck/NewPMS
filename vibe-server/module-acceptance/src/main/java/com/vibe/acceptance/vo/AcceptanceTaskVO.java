package com.vibe.acceptance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 验收任务 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "验收任务")
public class AcceptanceTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "适用的验收标准ID")
    private Long standardId;

    @Schema(description = "验收任务名称")
    private String name;

    @Schema(description = "验收申请人ID")
    private Long applyUserId;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "内部技术审核人ID")
    private Long internalAuditUserId;

    @Schema(description = "内部审核时间")
    private LocalDateTime internalAuditTime;

    @Schema(description = "内部审核结果 PASS/REJECT")
    private String internalAuditResult;

    @Schema(description = "客户签核链接")
    private String customerSignLink;

    @Schema(description = "客户签核人姓名")
    private String customerSignUser;

    @Schema(description = "客户签核时间")
    private LocalDateTime customerSignTime;

    @Schema(description = "客户签核结果 PASS/CONDITIONAL_PASS/REJECT")
    private String customerSignResult;

    @Schema(description = "自动评分")
    private BigDecimal score;

    @Schema(description = "状态 DRAFT/APPLIED/INTERNAL_AUDITED/CUSTOMER_SIGNING/COMPLETED/REJECTED")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
