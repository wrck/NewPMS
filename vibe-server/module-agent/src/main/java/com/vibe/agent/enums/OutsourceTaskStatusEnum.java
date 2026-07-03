package com.vibe.agent.enums;

import com.vibe.agent.constant.AgentConstant;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 转包任务状态枚举（状态机定义）
 *
 * <p>状态流转：</p>
 * <pre>
 * PENDING(待接单)
 *     ├── 接单 → ACCEPTED(已接单)
 *     └── 拒绝 → REJECTED(已拒绝)
 *
 * ACCEPTED(已接单) → IN_PROGRESS(执行中)    [代理商指派工程师]
 *
 * IN_PROGRESS(执行中)
 *     ├── 提交交付物 → SUBMITTED(待审核)
 *     └── 超期 → OVERDUE(已超期)
 *
 * SUBMITTED(待审核)
 *     ├── 审核通过 → CONFIRMED(已确认)
 *     ├── 审核退回 → RETURNED(已退回) → 回到 IN_PROGRESS
 *     └── 超期 → OVERDUE(已超期)
 *
 * 任意非终态 → OVERDUE(定时任务标记)
 *
 * 终态：REJECTED / CONFIRMED / OVERDUE
 * </pre>
 *
 * @author vibe
 */
@Getter
public enum OutsourceTaskStatusEnum {

    PENDING(AgentConstant.TASK_PENDING, "待接单"),
    ACCEPTED(AgentConstant.TASK_ACCEPTED, "已接单"),
    REJECTED(AgentConstant.TASK_REJECTED, "已拒绝"),
    IN_PROGRESS(AgentConstant.TASK_IN_PROGRESS, "执行中"),
    SUBMITTED(AgentConstant.TASK_SUBMITTED, "待审核"),
    CONFIRMED(AgentConstant.TASK_CONFIRMED, "已确认"),
    RETURNED(AgentConstant.TASK_RETURNED, "已退回"),
    OVERDUE(AgentConstant.TASK_OVERDUE, "已超期");

    /** 状态码（与数据库存储一致） */
    private final String code;
    /** 状态描述 */
    private final String description;

    OutsourceTaskStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 解析枚举
     */
    public static OutsourceTaskStatusEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (OutsourceTaskStatusEnum s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 判断当前状态是否可以流转到目标状态。
     *
     * <p>合法流转：</p>
     * <ul>
     *   <li>PENDING → ACCEPTED / REJECTED / OVERDUE</li>
     *   <li>ACCEPTED → IN_PROGRESS / OVERDUE</li>
     *   <li>IN_PROGRESS → SUBMITTED / OVERDUE</li>
     *   <li>SUBMITTED → CONFIRMED / RETURNED / OVERDUE</li>
     *   <li>RETURNED → IN_PROGRESS / OVERDUE</li>
     *   <li>REJECTED / CONFIRMED / OVERDUE → 终态，不可再流转</li>
     * </ul>
     *
     * @param target 目标状态
     * @return true 表示允许流转
     */
    public boolean canTransitionTo(OutsourceTaskStatusEnum target) {
        if (target == null) {
            return false;
        }
        return allowedTransitions().contains(target);
    }

    /**
     * 获取当前状态允许流转的下一状态集合。
     */
    private Set<OutsourceTaskStatusEnum> allowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(ACCEPTED, REJECTED, OVERDUE);
            case ACCEPTED -> EnumSet.of(IN_PROGRESS, OVERDUE);
            case IN_PROGRESS -> EnumSet.of(SUBMITTED, OVERDUE);
            case SUBMITTED -> EnumSet.of(CONFIRMED, RETURNED, OVERDUE);
            case RETURNED -> EnumSet.of(IN_PROGRESS, OVERDUE);
            // 终态：不允许再流转
            case REJECTED, CONFIRMED, OVERDUE -> EnumSet.noneOf(OutsourceTaskStatusEnum.class);
        };
    }

    /**
     * 是否终态（不可再流转）。
     */
    public boolean isTerminal() {
        return this == REJECTED || this == CONFIRMED || this == OVERDUE;
    }
}
