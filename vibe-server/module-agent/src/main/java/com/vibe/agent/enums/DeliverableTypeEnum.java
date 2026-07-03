package com.vibe.agent.enums;

import com.vibe.agent.constant.AgentConstant;
import lombok.Getter;

/**
 * 交付物类型枚举
 *
 * @author vibe
 */
@Getter
public enum DeliverableTypeEnum {

    PHOTO(AgentConstant.DELIVERABLE_PHOTO, "施工照片"),
    TEST_RECORD(AgentConstant.DELIVERABLE_TEST_RECORD, "测试记录"),
    RECEIPT(AgentConstant.DELIVERABLE_RECEIPT, "签收单"),
    CONFIG(AgentConstant.DELIVERABLE_CONFIG, "配置文件"),
    OTHER(AgentConstant.DELIVERABLE_OTHER, "其他");

    private final String code;
    private final String description;

    DeliverableTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DeliverableTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (DeliverableTypeEnum t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
