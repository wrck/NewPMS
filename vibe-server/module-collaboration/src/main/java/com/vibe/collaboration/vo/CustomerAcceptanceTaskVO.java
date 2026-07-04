package com.vibe.collaboration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户视角的验收任务 VO（脱敏）
 *
 * <p>仅暴露客户可见字段，不包含内部审核人ID/评分等内部细节。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户验收任务视图")
public class CustomerAcceptanceTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "验收任务ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "验收任务名称")
    private String name;

    @Schema(description = "状态 DRAFT/APPLIED/INTERNAL_AUDITED/CUSTOMER_SIGNING/COMPLETED/REJECTED")
    private String status;

    @Schema(description = "申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    @Schema(description = "内部审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime internalAuditTime;

    @Schema(description = "客户签核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime customerSignTime;

    @Schema(description = "客户签核结果 PASS/CONDITIONAL_PASS/REJECT/null")
    private String customerSignResult;

    @Schema(description = "客户签核意见")
    private String customerSignRemark;

    @Schema(description = "测试记录列表")
    private List<CustomerTestRecordVO> testRecords;

    /**
     * 客户视角的测试记录 VO
     */
    @Data
    @Schema(description = "客户测试记录视图")
    public static class CustomerTestRecordVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "记录ID")
        private Long id;

        @Schema(description = "测试类型 FUNCTION/PERFORMANCE/REDUNDANCY/OTHER")
        private String testType;

        @Schema(description = "测试项名称")
        private String testName;

        @Schema(description = "测试结果 PENDING/PASS/FAIL/NA")
        private String testResult;

        @Schema(description = "测试值")
        private String testValue;

        @Schema(description = "测试时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime testTime;

        @Schema(description = "备注")
        private String remark;
    }
}
