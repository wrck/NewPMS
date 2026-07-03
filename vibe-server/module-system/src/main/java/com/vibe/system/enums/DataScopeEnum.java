package com.vibe.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限范围
 *
 * @author vibe
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum {

    ALL("ALL", "全部数据"),
    DEPT("DEPT", "本部门数据"),
    SELF("SELF", "仅本人数据"),
    CUSTOM("CUSTOM", "自定义数据");

    private final String code;
    private final String desc;

    public static DataScopeEnum of(String code) {
        for (DataScopeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return ALL;
    }
}
