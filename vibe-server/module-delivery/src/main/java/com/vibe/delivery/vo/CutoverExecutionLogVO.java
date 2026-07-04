package com.vibe.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 割接操作日志 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "割接操作日志")
public class CutoverExecutionLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属割接方案ID")
    private Long planId;

    @Schema(description = "关联步骤ID")
    private Long stepId;

    @Schema(description = "步骤名称（联表查询）")
    private String stepName;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作动作")
    private String action;

    @Schema(description = "操作时间")
    private LocalDateTime logTime;

    @Schema(description = "操作内容")
    private String logContent;

    @Schema(description = "日志级别")
    private String logLevel;
}
