package com.vibe.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出差实体（business_trip）
 *
 * <p>差旅管理：出差申请、行程安排、费用记录、审批。
 * 状态流转：PENDING → APPROVED → COMPLETED / REJECTED。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("business_trip")
@Schema(description = "出差")
public class BusinessTripEntity extends ResourceBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long engineerId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联任务ID")
    private Long taskId;

    @Schema(description = "出发地")
    private String origin;

    @Schema(description = "目的地")
    private String destination;

    @Schema(description = "出差开始日期")
    private LocalDate startDate;

    @Schema(description = "出差结束日期")
    private LocalDate endDate;

    @Schema(description = "交通方式 PLANE/TRAIN/CAR/OTHER")
    private String transportMode;

    @Schema(description = "住宿信息")
    private String accommodation;

    @Schema(description = "预估费用")
    private BigDecimal estimatedCost;

    @Schema(description = "实际费用")
    private BigDecimal actualCost;

    @Schema(description = "出差事由")
    private String reason;

    @Schema(description = "状态 PENDING/APPROVED/REJECTED/COMPLETED")
    private String status;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "备注")
    private String remark;
}
