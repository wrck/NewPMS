package com.vibe.device.enums;

import lombok.Getter;

/**
 * 设备类别枚举（对应 device_model.category）。
 *
 * @author vibe
 */
@Getter
public enum DeviceCategory {

    ROUTER("路由器"),
    SWITCH("交换机"),
    AP("无线AP"),
    FIREWALL("防火墙"),
    WLC("无线控制器"),
    LB("负载均衡"),
    OTHER("其他");

    private final String displayName;

    DeviceCategory(String displayName) {
        this.displayName = displayName;
    }

    public static DeviceCategory parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return DeviceCategory.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
