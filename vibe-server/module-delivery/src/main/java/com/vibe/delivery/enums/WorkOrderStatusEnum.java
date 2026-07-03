package com.vibe.delivery.enums;

import lombok.Getter;

/**
 * 工单状态枚举
 *
 * <p>状态机：CREATED → CHECKED_IN → IN_PROGRESS → COMPLETED → CONFIRMED</p>
 *
 * @author vibe
 */
@Getter
public enum WorkOrderStatusEnum {

    CREATED("CREATED", "已创建"),
    CHECKED_IN("CHECKED_IN", "已签到"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成待确认"),
    CONFIRMED("CONFIRMED", "已确认");

    private final String code;
    private final String desc;

    WorkOrderStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static WorkOrderStatusEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (WorkOrderStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
