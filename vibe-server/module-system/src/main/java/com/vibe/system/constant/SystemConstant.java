package com.vibe.system.constant;

/**
 * 系统管理模块常量
 *
 * @author vibe
 */
public final class SystemConstant {

    private SystemConstant() {
    }

    /* ============ 用户状态 ============ */
    /** 用户状态-启用 */
    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    /** 用户状态-禁用 */
    public static final String USER_STATUS_DISABLED = "DISABLED";

    /* ============ 租户类型 ============ */
    public static final String TENANT_INTERNAL = "INTERNAL";
    public static final String TENANT_AGENT = "AGENT";
    public static final String TENANT_CUSTOMER = "CUSTOMER";

    /* ============ 菜单类型 ============ */
    /** 目录（仅用于分组，不出现在前端动态路由） */
    public static final String MENU_TYPE_DIRECTORY = "DIRECTORY";
    /** 菜单 */
    public static final String MENU_TYPE_MENU = "MENU";
    /** 按钮 */
    public static final String MENU_TYPE_BUTTON = "BUTTON";

    /* ============ 数据权限范围 ============ */
    public static final String DATA_SCOPE_ALL = "ALL";
    public static final String DATA_SCOPE_DEPT = "DEPT";
    public static final String DATA_SCOPE_SELF = "SELF";
    public static final String DATA_SCOPE_CUSTOM = "CUSTOM";

    /* ============ 启用/禁用（TINYINT） ============ */
    public static final Integer STATUS_ENABLED = 1;
    public static final Integer STATUS_DISABLED = 0;

    /* ============ 操作日志业务类型 ============ */
    public static final String BIZ_INSERT = "INSERT";
    public static final String BIZ_UPDATE = "UPDATE";
    public static final String BIZ_DELETE = "DELETE";
    public static final String BIZ_EXPORT = "EXPORT";
    public static final String BIZ_IMPORT = "IMPORT";
    public static final String BIZ_OTHER = "OTHER";

    /* ============ 通知类型 ============ */
    /** 通知 */
    public static final Integer NOTICE_TYPE_NOTICE = 1;
    /** 消息 */
    public static final Integer NOTICE_TYPE_MSG = 2;

    /* ============ 已读状态 ============ */
    public static final Integer READ_UNREAD = 0;
    public static final Integer READ_READ = 1;

    /* ============ 反馈类型 ============ */
    /** Bug 报告 */
    public static final String FEEDBACK_TYPE_BUG = "BUG";
    /** 功能建议 */
    public static final String FEEDBACK_TYPE_SUGGESTION = "SUGGESTION";
    /** 咨询 */
    public static final String FEEDBACK_TYPE_QUESTION = "QUESTION";

    /* ============ 反馈状态 ============ */
    /** 待处理 */
    public static final String FEEDBACK_STATUS_PENDING = "PENDING";
    /** 处理中 */
    public static final String FEEDBACK_STATUS_PROCESSING = "PROCESSING";
    /** 已解决 */
    public static final String FEEDBACK_STATUS_RESOLVED = "RESOLVED";
    /** 已关闭 */
    public static final String FEEDBACK_STATUS_CLOSED = "CLOSED";

    /* ============ 角色编码 ============ */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    /* ============ 默认密码（密码重置） ============ */
    public static final String DEFAULT_PASSWORD = "vibe@123";

    /* ============ 根节点父ID ============ */
    public static final Long ROOT_PARENT_ID = 0L;
}
