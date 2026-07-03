package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交付物列表项 VO
 *
 * <p>PM 首页待审核交付物列表展示。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "交付物列表项")
public class DeliverableItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "交付物ID")
    private Long deliverableId;

    @Schema(description = "转包任务ID")
    private Long outsourceTaskId;

    @Schema(description = "项目任务ID")
    private Long projectTaskId;

    @Schema(description = "交付物类型 PHOTO/TEST_RECORD/RECEIPT/CONFIG/OTHER")
    private String deliverableType;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "代理商公司ID")
    private Long agentCompanyId;

    @Schema(description = "代理商公司名称")
    private String agentCompanyName;

    @Schema(description = "转包任务状态")
    private String outsourceStatus;

    @Schema(description = "截止日期")
    private LocalDate deadline;
}
