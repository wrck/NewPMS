package com.vibe.project.enums;

import com.vibe.project.constant.ProjectConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 项目任务状态机枚举
 *
 * <p>PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → CONFIRMED</p>
 *
 * @author vibe
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    PENDING(ProjectConstant.TASK_PENDING, "待分配"),
    ASSIGNED(ProjectConstant.TASK_ASSIGNED, "已分配"),
    IN_PROGRESS(ProjectConstant.TASK_IN_PROGRESS, "进行中"),
    COMPLETED(ProjectConstant.TASK_COMPLETED, "已完成"),
    CONFIRMED(ProjectConstant.TASK_CONFIRMED, "已确认");

    private final String code;
    private final String desc;

    private static final Map<String, TaskStatusEnum> CODE_MAP = new HashMap<>();
    static {
        for (TaskStatusEnum e : values()) {
            CODE_MAP.put(e.code, e);
        }
    }

    public static TaskStatusEnum of(String code) {
        return code == null ? null : CODE_MAP.get(code);
    }

    public boolean isTerminal() {
        return this == CONFIRMED;
    }

    /**
     * 判断是否允许流转到目标状态。
     *
     * <p>流转规则：</p>
     * <ul>
     *   <li>PENDING → ASSIGNED</li>
     *   <li>ASSIGNED → IN_PROGRESS / PENDING（退回）</li>
     *   <li>IN_PROGRESS → COMPLETED / ASSIGNED（退回）</li>
     *   <li>COMPLETED → CONFIRMED / IN_PROGRESS（驳回返工）</li>
     *   <li>CONFIRMED → 终态</li>
     * </ul>
     */
    public boolean canTransitionTo(TaskStatusEnum target) {
        if (target == null || this == target) {
            return false;
        }
        Set<TaskStatusEnum> allowed = ALLOWED_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    private static final Map<TaskStatusEnum, Set<TaskStatusEnum>> ALLOWED_TRANSITIONS = new HashMap<>();
    static {
        ALLOWED_TRANSITIONS.put(PENDING, Set.of(ASSIGNED));
        ALLOWED_TRANSITIONS.put(ASSIGNED, Set.of(IN_PROGRESS, PENDING));
        ALLOWED_TRANSITIONS.put(IN_PROGRESS, Set.of(COMPLETED, ASSIGNED));
        ALLOWED_TRANSITIONS.put(COMPLETED, Set.of(CONFIRMED, IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(CONFIRMED, Collections.emptySet());
    }
}
