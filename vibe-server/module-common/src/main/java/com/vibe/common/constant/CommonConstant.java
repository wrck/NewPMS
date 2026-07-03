package com.vibe.common.constant;

/**
 * 系统通用常量
 *
 * @author vibe
 */
public final class CommonConstant {

    private CommonConstant() {
    }

    /* ============ 系统基础 ============ */
    /** 系统名称 */
    public static final String SYSTEM_NAME = "vibe";
    /** 系统版本 */
    public static final String SYSTEM_VERSION = "1.0.0";
    /** API 版本前缀 */
    public static final String API_PREFIX = "/api/v1";

    /* ============ 字符集 ============ */
    public static final String UTF_8 = "UTF-8";

    /* ============ 通用字符串 ============ */
    public static final String EMPTY = "";
    public static final String COMMA = ",";
    public static final String SLASH = "/";
    public static final String COLON = ":";
    public static final String UNDERLINE = "_";
    public static final String DASH = "-";

    /* ============ 逻辑删除 ============ */
    public static final Integer NOT_DELETED = 0;
    public static final Integer DELETED = 1;

    /* ============ 启用/禁用 ============ */
    public static final Integer ENABLED = 1;
    public static final Integer DISABLED = 0;

    /* ============ 是/否 ============ */
    public static final Integer YES = 1;
    public static final Integer NO = 0;

    /* ============ HTTP Header ============ */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_TENANT_TYPE = "X-Tenant-Type";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_CLIENT_TYPE = "X-Client-Type";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ORG_ID = "X-Org-Id";
    public static final String BEARER_PREFIX = "Bearer ";

    /* ============ 客户端类型 ============ */
    public static final String CLIENT_TYPE_PC = "PC";
    public static final String CLIENT_TYPE_MOBILE = "MOBILE";
    public static final String CLIENT_TYPE_AGENT = "AGENT";
    public static final String CLIENT_TYPE_CUSTOMER = "CUSTOMER";

    /* ============ 租户类型 ============ */
    public static final String TENANT_TYPE_INTERNAL = "INTERNAL";
    public static final String TENANT_TYPE_AGENT = "AGENT";
    public static final String TENANT_TYPE_CUSTOMER = "CUSTOMER";

    /* ============ Token 有效期（秒） ============ */
    /** PC Token 有效期 8h */
    public static final long TOKEN_TTL_PC = 8 * 60 * 60L;
    /** 移动端 Token 有效期 7d */
    public static final long TOKEN_TTL_MOBILE = 7 * 24 * 60 * 60L;
    /** 客户临时 Token 有效期 2h */
    public static final long TOKEN_TTL_CUSTOMER = 2 * 60 * 60L;
    /** Token 续期阈值 2h（剩余有效期小于该值时自动续签） */
    public static final long TOKEN_RENEW_THRESHOLD = 2 * 60 * 60L;

    /* ============ 时间格式 ============ */
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DATE_FORMAT_COMPACT = "yyyyMMdd";
    public static final String DATE_TIME_FORMAT_COMPACT = "yyyyMMddHHmmss";

    /* ============ 缓存默认 TTL（秒） ============ */
    public static final long DEFAULT_CACHE_TTL = 30 * 60L;
    public static final long SHORT_CACHE_TTL = 60L;
    public static final long MEDIUM_CACHE_TTL = 5 * 60L;
    public static final long LONG_CACHE_TTL = 60 * 60L;

    /* ============ 分页默认 ============ */
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 500;

    /* ============ 项目编号前缀 ============ */
    public static final String PROJECT_CODE_PREFIX = "PRJ";
}
