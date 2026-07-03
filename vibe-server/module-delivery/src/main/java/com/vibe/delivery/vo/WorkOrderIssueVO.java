package com.vibe.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单异常问题视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "工单异常问题")
public class WorkOrderIssueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题ID")
    private Long id;

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "问题类型")
    private String issueType;

    @Schema(description = "严重程度")
    private String severity;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "问题照片预签名 URL 列表")
    private List<String> photoUrls;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "解决时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建人姓名")
    private String createByName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
