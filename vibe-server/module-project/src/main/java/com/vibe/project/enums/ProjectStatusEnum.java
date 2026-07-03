package com.vibe.project.enums;

import com.vibe.project.constant.ProjectConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 项目状态机枚举
 *
 * <p>主流程：INIT → PLAN → EXECUTE → ACCEPT → CLOSE → ARCHIVED</p>
 * <p>支路：任意阶段 → ON_HOLD / CANCELLED（终态）</p>
 *
 * @author vibe
 */
@Getter
@AllArgsConstructor
public enum ProjectStatusEnum {

    INIT(ProjectConstant.STATUS_INIT, "立项"),
    PLAN(ProjectConstant.STATUS_PLAN, "规划中"),
    EXECUTE(ProjectConstant.STATUS_EXECUTE, "执行中"),
    ACCEPT(ProjectConstant.STATUS_ACCEPT, "验收中"),
    CLOSE(ProjectConstant.STATUS_CLOSE, "已结项"),
    ARCHIVED(ProjectConstant.STATUS_ARCHIVED, "已归档"),
    ON_HOLD(ProjectConstant.STATUS_ON_HOLD, "暂停"),
    CANCELLED(ProjectConstant.STATUS_CANCELLED, "已取消");

    private final String code;
    private final String desc;

    private static final Map<String, ProjectStatusEnum> CODE_MAP = new HashMap<>();
    static {
        for (ProjectStatusEnum e : values()) {
            CODE_MAP.put(e.code, e);
        }
    }

    public static ProjectStatusEnum of(String code) {
        return code == null ? null : CODE_MAP.get(code);
    }

    /**
     * 判断是否为终态（不可再流转）
     */
    public boolean isTerminal() {
        return this == ARCHIVED || this == CANCELLED;
    }

    /**
     * 判断是否允许流转到目标状态。
     *
     * <p>流转规则：</p>
     * <ul>
     *   <li>INIT → PLAN / ON_HOLD / CANCELLED</li>
     *   <li>PLAN → EXECUTE / ON_HOLD / CANCELLED</li>
     *   <li>EXECUTE → ACCEPT / ON_HOLD / CANCELLED</li>
     *   <li>ACCEPT → CLOSE / EXECUTE（驳回返工）/ ON_HOLD / CANCELLED</li>
     *   <li>CLOSE → ARCHIVED / ON_HOLD / CANCELLED</li>
     *   <li>ON_HOLD → 回到前一阶段（INIT/PLAN/EXECUTE/ACCEPT/CLOSE） / CANCELLED</li>
     *   <li>ARCHIVED / CANCELLED → 终态，不允许任何流转</li>
     * </ul>
     */
    public boolean canTransitionTo(ProjectStatusEnum target) {
        if (target == null || this == target) {
            return false;
        }
        if (isTerminal()) {
            return false;
        }
        Set<ProjectStatusEnum> allowed = ALLOWED_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    /**
     * 各状态允许流转的目标状态集合
     */
    private static final Map<ProjectStatusEnum, Set<ProjectStatusEnum>> ALLOWED_TRANSITIONS = new HashMap<>();
    static {
        ALLOWED_TRANSITIONS.put(INIT, setOf(PLAN, ON_HOLD, CANCELLED));
        ALLOWED_TRANSITIONS.put(PLAN, setOf(EXECUTE, ON_HOLD, CANCELLED));
        ALLOWED_TRANSITIONS.put(EXECUTE, setOf(ACCEPT, ON_HOLD, CANCELLED));
        ALLOWED_TRANSITIONS.put(ACCEPT, setOf(CLOSE, EXECUTE, ON_HOLD, CANCELLED));
        ALLOWED_TRANSITIONS.put(CLOSE, setOf(ARCHIVED, ON_HOLD, CANCELLED));
        // ON_HOLD 可回到主流程任一阶段（业务层应结合暂停前状态判断，此处放宽）
        ALLOWED_TRANSITIONS.put(ON_HOLD, setOf(INIT, PLAN, EXECUTE, ACCEPT, CLOSE, CANCELLED));
        ALLOWED_TRANSITIONS.put(ARCHIVED, Collections.emptySet());
        ALLOWED_TRANSITIONS.put(CANCELLED, Collections.emptySet());
    }

    @SafeVarargs
    private static Set<ProjectStatusEnum> setOf(ProjectStatusEnum... es) {
        return Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(es)));
    }
}
