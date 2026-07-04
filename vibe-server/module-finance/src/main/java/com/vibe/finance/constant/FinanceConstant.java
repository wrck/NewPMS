package com.vibe.finance.constant;

/**
 * 财务核算模块常量
 *
 * @author vibe
 */
public final class FinanceConstant {

    private FinanceConstant() {}

    /** 预算审批状态：草稿 */
    public static final String BUDGET_STATUS_DRAFT = "DRAFT";
    /** 预算审批状态：待审批 */
    public static final String BUDGET_STATUS_PENDING = "PENDING";
    /** 预算审批状态：已通过 */
    public static final String BUDGET_STATUS_APPROVED = "APPROVED";
    /** 预算审批状态：已驳回 */
    public static final String BUDGET_STATUS_REJECTED = "REJECTED";

    /** 成本类型：人工 */
    public static final String COST_TYPE_LABOR = "LABOR";
    /** 成本类型：差旅 */
    public static final String COST_TYPE_TRAVEL = "TRAVEL";
    /** 成本类型：代理商 */
    public static final String COST_TYPE_AGENT = "AGENT";
    /** 成本类型：其他 */
    public static final String COST_TYPE_OTHER = "OTHER";

    /** 结算审批状态：草稿 */
    public static final String SETTLEMENT_STATUS_DRAFT = "DRAFT";
    /** 结算审批状态：PM已确认 */
    public static final String SETTLEMENT_STATUS_PM_CONFIRMED = "PM_CONFIRMED";
    /** 结算审批状态：代理商已确认 */
    public static final String SETTLEMENT_STATUS_AGENT_CONFIRMED = "AGENT_CONFIRMED";
    /** 结算审批状态：待审批 */
    public static final String SETTLEMENT_STATUS_PENDING = "PENDING";
    /** 结算审批状态：总监已审批 */
    public static final String SETTLEMENT_STATUS_DIRECTOR_APPROVED = "DIRECTOR_APPROVED";
    /** 结算审批状态：财务已审批 */
    public static final String SETTLEMENT_STATUS_FINANCE_APPROVED = "FINANCE_APPROVED";
    /** 结算审批状态：已驳回 */
    public static final String SETTLEMENT_STATUS_REJECTED = "REJECTED";

    /** 付款状态：未付款 */
    public static final String PAYMENT_STATUS_UNPAID = "UNPAID";
    /** 付款状态：付款中 */
    public static final String PAYMENT_STATUS_PAYING = "PAYING";
    /** 付款状态：已付款 */
    public static final String PAYMENT_STATUS_PAID = "PAID";
}
