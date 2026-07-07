package com.vibe.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单签到 DTO（含 GPS 定位 + 签到照片）
 *
 * <p>签到照片由前端通过 multipart 上传，Controller 接收 MultipartFile 后传入 Service。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "工单签到")
public class WorkOrderCheckinDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "经度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "经度不能为空")
    private Double longitude;

    @Schema(description = "纬度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "纬度不能为空")
    private Double latitude;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "签到备注")
    private String remark;
}
