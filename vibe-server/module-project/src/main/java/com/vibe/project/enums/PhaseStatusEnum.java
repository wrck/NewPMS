package com.vibe.project.enums;

import com.vibe.project.constant.ProjectConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 项目阶段状态枚举
 *
 * @author vibe
 */
@Getter
@AllArgsConstructor
public enum PhaseStatusEnum {

    NOT_STARTED(ProjectConstant.PHASE_NOT_STARTED, "未开始"),
    IN_PROGRESS(ProjectConstant.PHASE_IN_PROGRESS, "进行中"),
    COMPLETED(ProjectConstant.PHASE_COMPLETED, "已完成");

    private final String code;
    private final String desc;

    private static final Map<String, PhaseStatusEnum> CODE_MAP = new HashMap<>();
    static {
        for (PhaseStatusEnum e : values()) {
            CODE_MAP.put(e.code, e);
        }
    }

    public static PhaseStatusEnum of(String code) {
        return code == null ? null : CODE_MAP.get(code);
    }

    public boolean canTransitionTo(PhaseStatusEnum target) {
        if (target == null || this == target) {
            return false;
        }
        Set<PhaseStatusEnum> allowed = ALLOWED.get(this);
        return allowed != null && allowed.contains(target);
    }

    private static final Map<PhaseStatusEnum, Set<PhaseStatusEnum>> ALLOWED = new HashMap<>();
    static {
        ALLOWED.put(NOT_STARTED, EnumSet.of(IN_PROGRESS));
        ALLOWED.put(IN_PROGRESS, EnumSet.of(COMPLETED, NOT_STARTED));
        ALLOWED.put(COMPLETED, EnumSet.of(IN_PROGRESS));
    }
}
