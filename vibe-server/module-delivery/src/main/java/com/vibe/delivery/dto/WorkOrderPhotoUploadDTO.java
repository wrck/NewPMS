package com.vibe.delivery.dto;

import com.vibe.delivery.bo.GpsLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 施工照片上传元数据 DTO（与 MultipartFile[] 配合使用）
 *
 * <p>前端上传照片时，除了文件本身，还需提交 GPS、拍摄时间、关联步骤等元数据。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "施工照片上传元数据")
public class WorkOrderPhotoUploadDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联步骤ID（可选，不传则归到工单通用照片）")
    private Long stepId;

    @Schema(description = "GPS 定位信息")
    private GpsLocation gps;

    @Schema(description = "拍摄时间")
    private LocalDateTime takenTime;
}
