package com.vibe.device.enums;

import lombok.Getter;

/**
 * 备件操作类型枚举（对应 spare_part_log.action_type）。
 *
 * @author vibe
 */
@Getter
public enum SparePartActionType {

    /** 入库 */
    IN,
    /** 领用 */
    OUT,
    /** 归还 */
    RETURN,
    /** 返修 */
    REPAIR;

    public static SparePartActionType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return SparePartActionType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
