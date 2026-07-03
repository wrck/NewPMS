package com.vibe.delivery.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * GPS 定位信息 BO（用于 work_order.checkin_location / checkout_location / work_order_photo.gps JSON 字段映射）
 *
 * @author vibe
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsLocation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;

    /** 详细地址（可选，由前端逆地理编码得到） */
    private String address;

    /** 与客户现场距离（米，仅用于响应展示，不持久化签到场景） */
    private Double distanceMeters;

    /** 拍照/签到时间（yyyy-MM-dd HH:mm:ss，水印用） */
    private String timeText;
}
