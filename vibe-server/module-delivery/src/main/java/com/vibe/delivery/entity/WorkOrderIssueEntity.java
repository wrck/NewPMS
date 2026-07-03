package com.vibe.delivery.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单异常问题实体（work_order_issue 表）
 *
 * <p>photos 为 JSON 数组字段（MinIO objectName 列表）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "work_order_issue", autoResultMap = true)
@Schema(description = "工单异常问题")
public class WorkOrderIssueEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "问题类型")
    private String issueType;

    @Schema(description = "严重程度 MINOR/MAJOR/BLOCKING")
    private String severity;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "问题照片地址列表")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> photos;

    @Schema(description = "状态 OPEN/PROCESSING/RESOLVED/CLOSED")
    private String status;

    @Schema(description = "解决时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "备注")
    private String remark;
}
