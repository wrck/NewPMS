package com.vibe.common.constant;

/**
 * Redis Key 命名常量
 *
 * <p>命名规范：{系统}:{模块}:{业务}:{标识}</p>
 *
 * <p>所有 Redis Key 应在此集中声明，避免散落在业务代码中造成冲突。</p>
 *
 * @author vibe
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
    }

    /** 系统前缀 */
    public static final String PREFIX = "vibe";

    /* ============ 分隔符 ============ */
    public static final String SEP = ":";

    /* ============ 认证相关 ============ */
    /** 用户 Token（key 后缀 userId），TTL 8h */
    public static final String AUTH_TOKEN = PREFIX + SEP + "auth" + SEP + "token" + SEP;
    /** 用户权限缓存（key 后缀 userId），TTL 30min */
    public static final String AUTH_PERM = PREFIX + SEP + "auth" + SEP + "perm" + SEP;
    /** Token 黑名单（key 后缀 tokenId） */
    public static final String AUTH_TOKEN_BLACKLIST = PREFIX + SEP + "auth" + SEP + "blacklist" + SEP;
    /** 用户登录信息（key 后缀 userId） */
    public static final String AUTH_LOGIN_INFO = PREFIX + SEP + "auth" + SEP + "login" + SEP;
    /** 短信验证码（key 后缀 手机号） */
    public static final String AUTH_SMS_CODE = PREFIX + SEP + "auth" + SEP + "sms" + SEP;
    /** 图形验证码（key 后缀 sessionId） */
    public static final String AUTH_CAPTCHA = PREFIX + SEP + "auth" + SEP + "captcha" + SEP;

    /* ============ 项目模块 ============ */
    /** 项目详情缓存（key 后缀 projectId），TTL 5min */
    public static final String PROJECT_DETAIL = PREFIX + SEP + "project" + SEP + "detail" + SEP;
    /** 项目列表缓存（key 后缀 用户ID） */
    public static final String PROJECT_LIST = PREFIX + SEP + "project" + SEP + "list" + SEP;
    /** 项目编号生成器（key 后缀 yyyyMM） */
    public static final String PROJECT_CODE_SEQ = PREFIX + SEP + "project" + SEP + "seq" + SEP;

    /* ============ 设备模块 ============ */
    /** 设备状态缓存（key 后缀 deviceId），TTL 1min */
    public static final String DEVICE_STATUS = PREFIX + SEP + "device" + SEP + "status" + SEP;
    /** 设备库存预警（key 后缀 仓库ID:型号ID） */
    public static final String DEVICE_STOCK_ALERT = PREFIX + SEP + "device" + SEP + "stock" + SEP + "alert" + SEP;

    /* ============ 资源调度模块 ============ */
    /** 工程师排期缓存（key 后缀 userId），TTL 5min */
    public static final String RESOURCE_CALENDAR = PREFIX + SEP + "resource" + SEP + "calendar" + SEP;
    /** 工程师技能缓存 */
    public static final String RESOURCE_ENGINEER_SKILL = PREFIX + SEP + "resource" + SEP + "skill" + SEP;

    /* ============ 代理商模块 ============ */
    /** 代理商任务列表缓存（key 后缀 agentId），TTL 5min */
    public static final String AGENT_TASK = PREFIX + SEP + "agent" + SEP + "task" + SEP;
    /** 代理商评分缓存 */
    public static final String AGENT_SCORE = PREFIX + SEP + "agent" + SEP + "score" + SEP;

    /* ============ 集成模块 ============ */
    /** 集成接口限流计数器（key 后缀 api） */
    public static final String INTEGRATION_RATE_LIMIT = PREFIX + SEP + "integration" + SEP + "ratelimit" + SEP;
    /** 集成熔断状态 */
    public static final String INTEGRATION_CIRCUIT = PREFIX + SEP + "integration" + SEP + "circuit" + SEP;

    /* ============ 系统模块 ============ */
    /** 数据字典缓存（key 后缀 dictType） */
    public static final String SYS_DICT = PREFIX + SEP + "sys" + SEP + "dict" + SEP;
    /** 系统配置缓存（key 后缀 configKey） */
    public static final String SYS_CONFIG = PREFIX + SEP + "sys" + SEP + "config" + SEP;
    /** 菜单树缓存 */
    public static final String SYS_MENU = PREFIX + SEP + "sys" + SEP + "menu" + SEP;
    /** 在线用户集合 */
    public static final String ONLINE_USERS = PREFIX + SEP + "sys" + SEP + "online" + SEP + "users";

    /* ============ 分布式锁 ============ */
    public static final String LOCK_PREFIX = PREFIX + SEP + "lock" + SEP;

    /* ============ 构造完整 Key 的工具方法 ============ */

    public static String authToken(Long userId) {
        return AUTH_TOKEN + userId;
    }

    public static String authPerm(Long userId) {
        return AUTH_PERM + userId;
    }

    public static String tokenBlacklist(String tokenId) {
        return AUTH_TOKEN_BLACKLIST + tokenId;
    }

    public static String smsCode(String phone) {
        return AUTH_SMS_CODE + phone;
    }

    public static String captcha(String sessionId) {
        return AUTH_CAPTCHA + sessionId;
    }

    public static String projectDetail(Long projectId) {
        return PROJECT_DETAIL + projectId;
    }

    public static String projectCodeSeq(String yyyyMM) {
        return PROJECT_CODE_SEQ + yyyyMM;
    }

    public static String deviceStatus(Long deviceId) {
        return DEVICE_STATUS + deviceId;
    }

    public static String resourceCalendar(Long userId) {
        return RESOURCE_CALENDAR + userId;
    }

    public static String agentTask(Long agentId) {
        return AGENT_TASK + agentId;
    }

    public static String sysDict(String dictType) {
        return SYS_DICT + dictType;
    }

    public static String sysConfig(String configKey) {
        return SYS_CONFIG + configKey;
    }

    public static String lock(String business) {
        return LOCK_PREFIX + business;
    }
}
