package com.vibe.delivery.constant;

/**
 * 割接管理模块常量
 *
 * <p>定义割接方案/步骤的状态机、操作动作等常量。
 * 状态流转图见设计文档 2.6.2 割接管理。</p>
 *
 * @author vibe
 */
public final class CutoverConstant {

    private CutoverConstant() {
    }

    /* ============ 割接方案状态 ============ */
    /** 草稿（编制中） */
    public static final String PLAN_STATUS_DRAFT = "DRAFT";
    /** 待内部审批（PM 已提交） */
    public static final String PLAN_STATUS_PENDING_INTERNAL_APPROVAL = "PENDING_INTERNAL_APPROVAL";
    /** 内部审批通过 */
    public static final String PLAN_STATUS_INTERNAL_APPROVED = "INTERNAL_APPROVED";
    /** 内部审批驳回 */
    public static final String PLAN_STATUS_INTERNAL_REJECTED = "INTERNAL_REJECTED";
    /** 待客户审批（已发起客户签核链接） */
    public static final String PLAN_STATUS_PENDING_CUSTOMER_APPROVAL = "PENDING_CUSTOMER_APPROVAL";
    /** 客户审批通过 */
    public static final String PLAN_STATUS_CUSTOMER_APPROVED = "CUSTOMER_APPROVED";
    /** 客户审批驳回 */
    public static final String PLAN_STATUS_CUSTOMER_REJECTED = "CUSTOMER_REJECTED";
    /** 执行中 */
    public static final String PLAN_STATUS_EXECUTING = "EXECUTING";
    /** 已完成 */
    public static final String PLAN_STATUS_COMPLETED = "COMPLETED";
    /** 已中止（异常终止） */
    public static final String PLAN_STATUS_ABORTED = "ABORTED";

    /* ============ 割接步骤状态 ============ */
    /** 待执行 */
    public static final String STEP_STATUS_PENDING = "PENDING";
    /** 执行中 */
    public static final String STEP_STATUS_EXECUTING = "EXECUTING";
    /** 已完成 */
    public static final String STEP_STATUS_COMPLETED = "COMPLETED";
    /** 已回退 */
    public static final String STEP_STATUS_ROLLED_BACK = "ROLLED_BACK";
    /** 已中止 */
    public static final String STEP_STATUS_ABORTED = "ABORTED";

    /* ============ 操作动作（execution_log.action） ============ */
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_SUBMIT_INTERNAL_APPROVAL = "SUBMIT_INTERNAL_APPROVAL";
    public static final String ACTION_INTERNAL_APPROVE = "INTERNAL_APPROVE";
    public static final String ACTION_INTERNAL_REJECT = "INTERNAL_REJECT";
    public static final String ACTION_START_CUSTOMER_APPROVAL = "START_CUSTOMER_APPROVAL";
    public static final String ACTION_CUSTOMER_APPROVE = "CUSTOMER_APPROVE";
    public static final String ACTION_CUSTOMER_REJECT = "CUSTOMER_REJECT";
    public static final String ACTION_START_EXECUTION = "START_EXECUTION";
    public static final String ACTION_STEP_EXECUTE = "STEP_EXECUTE";
    public static final String ACTION_STEP_ROLLBACK = "STEP_ROLLBACK";
    public static final String ACTION_STEP_EXCEPTION = "STEP_EXCEPTION";
    public static final String ACTION_COMPLETE = "COMPLETE";
    public static final String ACTION_ABORT = "ABORT";

    /* ============ 日志级别 ============ */
    public static final String LOG_LEVEL_INFO = "INFO";
    public static final String LOG_LEVEL_WARN = "WARN";
    public static final String LOG_LEVEL_ERROR = "ERROR";

    /* ============ 客户签核结果 ============ */
    public static final String CUSTOMER_RESULT_APPROVED = "APPROVED";
    public static final String CUSTOMER_RESULT_REJECTED = "REJECTED";

    /* ============ 内部审批结果 ============ */
    public static final String INTERNAL_RESULT_APPROVED = "APPROVED";
    public static final String INTERNAL_RESULT_REJECTED = "REJECTED";
}
