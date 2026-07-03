package com.vibe.agent.constant;

/**
 * 代理商管理模块常量
 *
 * <p>定义代理商公司状态、工程师状态、转包任务状态、交付物类型、工作量状态、
 * 评分维度权重等常量，供 Service/Controller 统一引用。</p>
 *
 * @author vibe
 */
public final class AgentConstant {

    private AgentConstant() {
    }

    /* ============ 代理商公司状态 ============ */
    /** 活跃（合作中） */
    public static final String COMPANY_STATUS_ACTIVE = "ACTIVE";
    /** 暂停合作 */
    public static final String COMPANY_STATUS_SUSPENDED = "SUSPENDED";
    /** 终止合作 */
    public static final String COMPANY_STATUS_TERMINATED = "TERMINATED";

    /* ============ 代理商工程师状态 ============ */
    /** 启用 */
    public static final String ENGINEER_STATUS_ACTIVE = "ACTIVE";
    /** 停用 */
    public static final String ENGINEER_STATUS_DISABLED = "DISABLED";

    /* ============ 转包任务状态 ============ */
    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_ACCEPTED = "ACCEPTED";
    public static final String TASK_REJECTED = "REJECTED";
    public static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_SUBMITTED = "SUBMITTED";
    public static final String TASK_CONFIRMED = "CONFIRMED";
    public static final String TASK_RETURNED = "RETURNED";
    public static final String TASK_OVERDUE = "OVERDUE";

    /* ============ 交付物类型 ============ */
    /** 施工照片 */
    public static final String DELIVERABLE_PHOTO = "PHOTO";
    /** 测试记录 */
    public static final String DELIVERABLE_TEST_RECORD = "TEST_RECORD";
    /** 签收单 */
    public static final String DELIVERABLE_RECEIPT = "RECEIPT";
    /** 配置文件 */
    public static final String DELIVERABLE_CONFIG = "CONFIG";
    /** 其他 */
    public static final String DELIVERABLE_OTHER = "OTHER";

    /** 施工照片最少上传数量（必传校验） */
    public static final int MIN_PHOTO_COUNT = 3;

    /* ============ 工作量状态 ============ */
    public static final String WORKLOAD_SUBMITTED = "SUBMITTED";
    public static final String WORKLOAD_CONFIRMED = "CONFIRMED";
    public static final String WORKLOAD_REJECTED = "REJECTED";

    /* ============ 评分维度权重（综合评分计算） ============ */
    /** 交付及时性权重 30% */
    public static final double WEIGHT_TIMELINESS = 0.30;
    /** 交付质量权重 30% */
    public static final double WEIGHT_QUALITY = 0.30;
    /** 沟通协作权重 20% */
    public static final double WEIGHT_COMMUNICATION = 0.20;
    /** 问题处理权重 20% */
    public static final double WEIGHT_ISSUE = 0.20;

    /* ============ 角色编码（与 sys_role 对齐） ============ */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_DIRECTOR = "DIRECTOR";
    public static final String ROLE_PM = "PM";
    public static final String ROLE_AGENT_ADMIN = "AGENT_ADMIN";
    public static final String ROLE_AGENT_ENGINEER = "AGENT_ENGINEER";

    /* ============ 模块名 ============ */
    public static final String MODULE_AGENT = "代理商管理";
    public static final String MODULE_OUTSOURCE_TASK = "转包任务";
    public static final String MODULE_DELIVERABLE = "交付物";
    public static final String MODULE_WORKLOAD = "工作量";
    public static final String MODULE_SCORE = "代理商评分";
}
