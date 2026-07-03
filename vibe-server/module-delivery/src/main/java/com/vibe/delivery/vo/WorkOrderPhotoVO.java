package com.vibe.delivery.vo;

import com.vibe.delivery.bo.GpsLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单施工照片视图对象
 *
 * <p>photoUrl / thumbnailUrl 返回 MinIO 预签名 URL，原 objectName 不直接暴露。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "工单施工照片")
public class WorkOrderPhotoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "照片ID")
    private Long id;

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "关联步骤ID")
    private Long stepId;

    @Schema(description = "照片预签名 URL")
    private String photoUrl;

    @Schema(description = "缩略图预签名 URL")
    private String thumbnailUrl;

    @Schema(description = "GPS 信息")
    private GpsLocation gps;

    @Schema(description = "拍摄时间")
    private LocalDateTime takenTime;

    @Schema(description = "上传人ID")
    private Long uploadedBy;

    @Schema(description = "上传人姓名")
    private String uploadedByName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
