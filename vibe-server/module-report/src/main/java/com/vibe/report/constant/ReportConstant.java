package com.vibe.report.constant;

/**
 * 报表分析模块常量
 *
 * <p>定义角色编码、风险类型、图表维度等常量，供 Service/Mapper 统一引用。
 * 角色编码与 sys_role 表对齐。</p>
 *
 * @author vibe
 */
public final class ReportConstant {

    private ReportConstant() {
    }

    /* ============ 角色编码（与 sys_role 对齐） ============ */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_DIRECTOR = "DIRECTOR";
    public static final String ROLE_PM = "PM";
    public static final String ROLE_ENGINEER = "ENGINEER";
    public static final String ROLE_AGENT_ADMIN = "AGENT_ADMIN";
    public static final String ROLE_AGENT_ENGINEER = "AGENT_ENGINEER";

    /* ============ 风险类型 ============ */
    /** 进度滞后：进度低于预期或长期未更新 */
    public static final String RISK_PROGRESS_DELAY = "PROGRESS_DELAY";
    /** 超期任务：存在已超过计划结束日期仍未完成的任务 */
    public static final String RISK_OVERDUE_TASK = "OVERDUE_TASK";
    /** 未解决问题：存在未关闭的问题记录 */
    public static final String RISK_UNRESOLVED_ISSUE = "UNRESOLVED_ISSUE";
    /** 项目超期：当前日期超过计划结束日期但未结项 */
    public static final String RISK_PROJECT_OVERDUE = "PROJECT_OVERDUE";

    /* ============ 风险类型展示名 ============ */
    public static final String RISK_NAME_PROGRESS_DELAY = "进度滞后";
    public static final String RISK_NAME_OVERDUE_TASK = "超期任务";
    public static final String RISK_NAME_UNRESOLVED_ISSUE = "未解决问题";
    public static final String RISK_NAME_PROJECT_OVERDUE = "项目超期";

    /* ============ 项目状态展示名 ============ */
    public static final String STATUS_NAME_INIT = "立项";
    public static final String STATUS_NAME_PLAN = "规划中";
    public static final String STATUS_NAME_EXECUTE = "执行中";
    public static final String STATUS_NAME_ACCEPT = "验收中";
    public static final String STATUS_NAME_CLOSE = "已结项";
    public static final String STATUS_NAME_ARCHIVED = "已归档";
    public static final String STATUS_NAME_ON_HOLD = "暂停";
    public static final String STATUS_NAME_CANCELLED = "已取消";

    /* ============ 工程师状态 ============ */
    public static final String ENGINEER_STATUS_ACTIVE = "ACTIVE";

    /* ============ 代理商公司状态 ============ */
    public static final String COMPANY_STATUS_ACTIVE = "ACTIVE";

    /* ============ 转包任务状态 ============ */
    public static final String OUTSOURCE_PENDING = "PENDING";
    public static final String OUTSOURCE_IN_PROGRESS = "IN_PROGRESS";
    public static final String OUTSOURCE_SUBMITTED = "SUBMITTED";

    /* ============ 任务状态 ============ */
    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_ASSIGNED = "ASSIGNED";
    public static final String TASK_IN_PROGRESS = "IN_PROGRESS";

    /* ============ 设备状态 ============ */
    public static final String DEVICE_ONLINE = "ONLINE";

    /* ============ 趋势查询月份数 ============ */
    public static final int TREND_MONTHS = 12;

    /* ============ 列表默认条数 ============ */
    public static final int DASHBOARD_LIST_SIZE = 5;
    public static final int RISK_LIST_SIZE = 20;
}
