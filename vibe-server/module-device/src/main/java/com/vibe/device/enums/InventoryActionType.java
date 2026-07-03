package com.vibe.device.enums;

import lombok.Getter;

/**
 * 设备出入库操作类型枚举（对应 device_inventory_log.action_type）。
 *
 * @author vibe
 */
@Getter
public enum InventoryActionType {

    /** 入库 */
    IN,
    /** 出库领用 */
    OUT,
    /** 退库归还 */
    RETURN,
    /** 调拨 */
    TRANSFER;

    public static InventoryActionType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return InventoryActionType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
