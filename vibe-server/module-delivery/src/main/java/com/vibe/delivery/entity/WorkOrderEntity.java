package com.vibe.delivery.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.vibe.common.base.BaseEntity;
import com.vibe.delivery.bo.GpsLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单实体（work_order 表，含 @Version 乐观锁）
 *
 * <p>checkin_location / checkout_location 为 JSON 字段，映射到 {@link GpsLocation}。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "work_order", autoResultMap = true)
@Schema(description = "工单")
public class WorkOrderEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联项目任务ID")
    private Long taskId;

    @Schema(description = "执行工程师ID（关联 sys_user.id）")
    private Long engineerId;

    @Schema(description = "签到时间")
    private LocalDateTime checkinTime;

    @Schema(description = "签退时间")
    private LocalDateTime checkoutTime;

    @Schema(description = "签到 GPS 坐标与地址")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private GpsLocation checkinLocation;

    @Schema(description = "签退 GPS 坐标与地址")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private GpsLocation checkoutLocation;

    @Schema(description = "签到照片地址（MinIO objectName）")
    private String checkinPhoto;

    @Schema(description = "工单状态 CREATED/CHECKED_IN/IN_PROGRESS/COMPLETED/CONFIRMED")
    private String status;

    @Schema(description = "总工时（小时）")
    private BigDecimal totalDuration;

    @Schema(description = "照片数量")
    private Integer photoCount;

    @Schema(description = "备注")
    private String remark;
}
