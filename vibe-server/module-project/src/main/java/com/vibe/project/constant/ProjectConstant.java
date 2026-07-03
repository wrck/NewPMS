package com.vibe.project.constant;

/**
 * 项目管理模块常量
 *
 * @author vibe
 */
public final class ProjectConstant {

    private ProjectConstant() {
    }

    /* ============ 项目状态 ============ */
    public static final String STATUS_INIT = "INIT";
    public static final String STATUS_PLAN = "PLAN";
    public static final String STATUS_EXECUTE = "EXECUTE";
    public static final String STATUS_ACCEPT = "ACCEPT";
    public static final String STATUS_CLOSE = "CLOSE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_ON_HOLD = "ON_HOLD";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /* ============ 阶段状态 ============ */
    public static final String PHASE_NOT_STARTED = "NOT_STARTED";
    public static final String PHASE_IN_PROGRESS = "IN_PROGRESS";
    public static final String PHASE_COMPLETED = "COMPLETED";

    /* ============ 任务状态 ============ */
    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_ASSIGNED = "ASSIGNED";
    public static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_COMPLETED = "COMPLETED";
    public static final String TASK_CONFIRMED = "CONFIRMED";

    /* ============ 执行模式 ============ */
    public static final String EXECUTE_MODE_SELF = "SELF";
    public static final String EXECUTE_MODE_AGENT = "AGENT";
    public static final String EXECUTE_MODE_MIXED = "MIXED";

    /* ============ 项目优先级 ============ */
    public static final String PRIORITY_P0 = "P0";
    public static final String PRIORITY_P1 = "P1";
    public static final String PRIORITY_P2 = "P2";
    public static final String PRIORITY_P3 = "P3";

    /* ============ 任务优先级 ============ */
    public static final String TASK_PRIORITY_HIGH = "HIGH";
    public static final String TASK_PRIORITY_MEDIUM = "MEDIUM";
    public static final String TASK_PRIORITY_LOW = "LOW";

    /* ============ 影响程度 / 发生概率 ============ */
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_LOW = "LOW";

    /* ============ 风险/问题状态 ============ */
    public static final String TRACK_OPEN = "OPEN";
    public static final String TRACK_PROCESSING = "PROCESSING";
    public static final String TRACK_RESOLVED = "RESOLVED";
    public static final String TRACK_CLOSED = "CLOSED";

    /* ============ 里程碑状态 ============ */
    public static final String MILESTONE_PENDING = "PENDING";
    public static final String MILESTONE_REACHED = "REACHED";
    public static final String MILESTONE_DELAYED = "DELAYED";

    /* ============ 变更状态 ============ */
    public static final String CHANGE_PENDING = "PENDING";
    public static final String CHANGE_APPROVED = "APPROVED";
    public static final String CHANGE_REJECTED = "REJECTED";
    public static final String CHANGE_EXECUTED = "EXECUTED";

    /* ============ 变更类型 ============ */
    public static final String CHANGE_TYPE_SCOPE = "SCOPE";
    public static final String CHANGE_TYPE_TIME = "TIME";
    public static final String CHANGE_TYPE_RESOURCE = "RESOURCE";
    public static final String CHANGE_TYPE_OTHER = "OTHER";

    /* ============ 任务类型 ============ */
    public static final String TASK_TYPE_SURVEY = "SURVEY";
    public static final String TASK_TYPE_INSTALL = "INSTALL";
    public static final String TASK_TYPE_DEBUG = "DEBUG";
    public static final String TASK_TYPE_CUTOVER = "CUTOVER";
    public static final String TASK_TYPE_ACCEPT = "ACCEPT";
    public static final String TASK_TYPE_OTHER = "OTHER";

    /* ============ 阶段编码 ============ */
    public static final String PHASE_CODE_SURVEY = "SURVEY";
    public static final String PHASE_CODE_DESIGN = "DESIGN";
    public static final String PHASE_CODE_DELIVER = "DELIVER";
    public static final String PHASE_CODE_INSTALL = "INSTALL";
    public static final String PHASE_CODE_DEBUG = "DEBUG";
    public static final String PHASE_CODE_ACCEPT = "ACCEPT";

    /* ============ 项目成员角色 ============ */
    public static final String MEMBER_ROLE_PM = "PM";
    public static final String MEMBER_ROLE_ENGINEER = "ENGINEER";
    public static final String MEMBER_ROLE_AGENT = "AGENT";
    public static final String MEMBER_ROLE_CUSTOMER = "CUSTOMER";

    /* ============ 模板启用/禁用 ============ */
    public static final Integer TEMPLATE_ENABLED = 1;
    public static final Integer TEMPLATE_DISABLED = 0;

    /* ============ 角色编码（与 sys_role 对齐） ============ */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_DIRECTOR = "DIRECTOR";
    public static final String ROLE_PM = "PM";
    public static final String ROLE_ENGINEER = "ENGINEER";
    public static final String ROLE_AGENT_ADMIN = "AGENT_ADMIN";
}
