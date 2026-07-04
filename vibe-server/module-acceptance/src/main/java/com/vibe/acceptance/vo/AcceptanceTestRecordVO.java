package com.vibe.acceptance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 验收测试记录 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "验收测试记录")
public class AcceptanceTestRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @Schema(description = "关联检查项ID")
    private Long itemId;

    @Schema(description = "测试类型 FUNCTION/PERFORMANCE/REDUNDANCY/OTHER")
    private String testType;

    @Schema(description = "测试项名称")
    private String testName;

    @Schema(description = "测试结果 PENDING/PASS/FAIL/NA")
    private String testResult;

    @Schema(description = "测试值")
    private String testValue;

    @Schema(description = "测试截图/证明材料URL")
    private String evidenceUrl;

    @Schema(description = "测试人ID")
    private Long testerId;

    @Schema(description = "测试时间")
    private LocalDateTime testTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
