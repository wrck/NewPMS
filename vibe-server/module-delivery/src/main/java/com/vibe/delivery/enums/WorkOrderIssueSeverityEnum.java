package com.vibe.delivery.enums;

import lombok.Getter;

/**
 * 异常问题严重程度枚举
 *
 * @author vibe
 */
@Getter
public enum WorkOrderIssueSeverityEnum {

    MINOR("MINOR", "轻微"),
    MAJOR("MAJOR", "严重"),
    BLOCKING("BLOCKING", "阻断");

    private final String code;
    private final String desc;

    WorkOrderIssueSeverityEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static WorkOrderIssueSeverityEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (WorkOrderIssueSeverityEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
