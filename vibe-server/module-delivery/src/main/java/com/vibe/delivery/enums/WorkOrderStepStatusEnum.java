package com.vibe.delivery.enums;

import lombok.Getter;

/**
 * 施工步骤状态枚举
 *
 * @author vibe
 */
@Getter
public enum WorkOrderStepStatusEnum {

    WAITING("WAITING", "待完成"),
    COMPLETED("COMPLETED", "已完成"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String desc;

    WorkOrderStepStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static WorkOrderStepStatusEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (WorkOrderStepStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
