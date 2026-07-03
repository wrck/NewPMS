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
import java.time.LocalDateTime;

/**
 * 工单施工照片实体（work_order_photo 表）
 *
 * <p>gps 为 JSON 字段，映射到 {@link GpsLocation}。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "work_order_photo", autoResultMap = true)
@Schema(description = "工单施工照片")
public class WorkOrderPhotoEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "关联步骤ID")
    private Long stepId;

    @Schema(description = "照片地址（MinIO objectName）")
    private String photoUrl;

    @Schema(description = "缩略图地址")
    private String thumbnailUrl;

    @Schema(description = "GPS 信息（经纬度+地址+水印时间）")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private GpsLocation gps;

    @Schema(description = "拍摄时间")
    private LocalDateTime takenTime;

    @Schema(description = "上传人ID")
    private Long uploadedBy;
}
