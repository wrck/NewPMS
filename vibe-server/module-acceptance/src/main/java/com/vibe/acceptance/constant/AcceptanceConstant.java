package com.vibe.acceptance.constant;

/**
 * 验收管理模块常量
 *
 * @author vibe
 */
public final class AcceptanceConstant {

    private AcceptanceConstant() {}

    /** 验收任务状态：草稿 */
    public static final String TASK_STATUS_DRAFT = "DRAFT";
    /** 验收任务状态：已申请 */
    public static final String TASK_STATUS_APPLIED = "APPLIED";
    /** 验收任务状态：内部审核通过 */
    public static final String TASK_STATUS_INTERNAL_AUDITED = "INTERNAL_AUDITED";
    /** 验收任务状态：客户签核中 */
    public static final String TASK_STATUS_CUSTOMER_SIGNING = "CUSTOMER_SIGNING";
    /** 验收任务状态：已完成 */
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";
    /** 验收任务状态：已驳回 */
    public static final String TASK_STATUS_REJECTED = "REJECTED";

    /** 遗留问题状态：待处理 */
    public static final String ISSUE_STATUS_OPEN = "OPEN";
    /** 遗留问题状态：整改中 */
    public static final String ISSUE_STATUS_IN_PROGRESS = "IN_PROGRESS";
    /** 遗留问题状态：已整改 */
    public static final String ISSUE_STATUS_RESOLVED = "RESOLVED";
    /** 遗留问题状态：已闭环 */
    public static final String ISSUE_STATUS_CLOSED = "CLOSED";

    /** 竣工文档类型：As-Built 网络拓扑图 */
    public static final String DOC_TYPE_TOPOLOGY = "TOPOLOGY";
    /** 竣工文档类型：设备清单 */
    public static final String DOC_TYPE_DEVICE_LIST = "DEVICE_LIST";
    /** 竣工文档类型：配置备份文件 */
    public static final String DOC_TYPE_CONFIG_BACKUP = "CONFIG_BACKUP";
    /** 竣工文档类型：测试报告 */
    public static final String DOC_TYPE_TEST_REPORT = "TEST_REPORT";
    /** 竣工文档类型：维护操作手册 */
    public static final String DOC_TYPE_MAINTENANCE_MANUAL = "MAINTENANCE_MANUAL";
}
