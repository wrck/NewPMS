package com.vibe.delivery.enums;

import lombok.Getter;

/**
 * 异常问题状态枚举
 *
 * <p>状态流转：OPEN → PROCESSING → RESOLVED → CLOSED</p>
 *
 * @author vibe
 */
@Getter
public enum WorkOrderIssueStatusEnum {

    OPEN("OPEN", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    RESOLVED("RESOLVED", "已解决"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String desc;

    WorkOrderIssueStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static WorkOrderIssueStatusEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (WorkOrderIssueStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
