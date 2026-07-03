package com.vibe.resource.constant;

/**
 * 资源调度模块常量
 *
 * @author vibe
 */
public final class ResourceConstant {

    private ResourceConstant() {
    }

    /* ============ 工程师状态 ============ */
    /** 在职 */
    public static final String ENGINEER_STATUS_ACTIVE = "ACTIVE";
    /** 离职 */
    public static final String ENGINEER_STATUS_RESIGNED = "RESIGNED";

    /* ============ 技能等级 ============ */
    public static final String SKILL_LEVEL_JUNIOR = "JUNIOR";
    public static final String SKILL_LEVEL_MIDDLE = "MIDDLE";
    public static final String SKILL_LEVEL_SENIOR = "SENIOR";
    public static final String SKILL_LEVEL_EXPERT = "EXPERT";

    /* ============ 排期类型 ============ */
    /** 任务 */
    public static final String SCHEDULE_TYPE_TASK = "TASK";
    /** 请假 */
    public static final String SCHEDULE_TYPE_LEAVE = "LEAVE";
    /** 培训 */
    public static final String SCHEDULE_TYPE_TRAINING = "TRAINING";
    /** 会议 */
    public static final String SCHEDULE_TYPE_MEETING = "MEETING";

    /* ============ 请假类型 ============ */
    public static final String LEAVE_TYPE_ANNUAL = "ANNUAL";
    public static final String LEAVE_TYPE_SICK = "SICK";
    public static final String LEAVE_TYPE_PERSONAL = "PERSONAL";
    public static final String LEAVE_TYPE_OTHER = "OTHER";

    /* ============ 请假/审批状态 ============ */
    public static final String APPROVAL_PENDING = "PENDING";
    public static final String APPROVAL_APPROVED = "APPROVED";
    public static final String APPROVAL_REJECTED = "REJECTED";
    public static final String TRIP_COMPLETED = "COMPLETED";

    /* ============ 工时状态 ============ */
    public static final String TIMESHEET_SUBMITTED = "SUBMITTED";
    public static final String TIMESHEET_APPROVED = "APPROVED";
    public static final String TIMESHEET_REJECTED = "REJECTED";

    /* ============ 任务状态（与 project_task 状态对齐） ============ */
    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_ASSIGNED = "ASSIGNED";
    public static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_COMPLETED = "COMPLETED";
    public static final String TASK_CONFIRMED = "CONFIRMED";

    /* ============ 交通方式 ============ */
    public static final String TRANSPORT_PLANE = "PLANE";
    public static final String TRANSPORT_TRAIN = "TRAIN";
    public static final String TRANSPORT_CAR = "CAR";
    public static final String TRANSPORT_OTHER = "OTHER";

    /* ============ 角色编码 ============ */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_DISPATCHER = "DISPATCHER";
    public static final String ROLE_ENGINEER = "ENGINEER";
    public static final String ROLE_PM = "PM";

    /* ============ 智能推荐权重 ============ */
    /** 技能匹配权重 40% */
    public static final double WEIGHT_SKILL = 0.4;
    /** 区域就近权重 30% */
    public static final double WEIGHT_REGION = 0.3;
    /** 当前负荷权重 30% */
    public static final double WEIGHT_WORKLOAD = 0.3;

    /* ============ 操作日志业务类型 ============ */
    public static final String BIZ_INSERT = "INSERT";
    public static final String BIZ_UPDATE = "UPDATE";
    public static final String BIZ_DELETE = "DELETE";
    public static final String BIZ_QUERY = "QUERY";
    public static final String BIZ_DISPATCH = "DISPATCH";
    public static final String BIZ_APPROVE = "APPROVE";
    public static final String BIZ_OTHER = "OTHER";
}
