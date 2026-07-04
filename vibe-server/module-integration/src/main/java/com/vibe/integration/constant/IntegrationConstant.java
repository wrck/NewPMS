package com.vibe.integration.constant;

/**
 * 集成管理常量
 *
 * @author vibe
 */
public final class IntegrationConstant {

    private IntegrationConstant() {
    }

    /** 调用状态 - 成功 */
    public static final String CALL_STATUS_SUCCESS = "SUCCESS";
    /** 调用状态 - 失败 */
    public static final String CALL_STATUS_FAIL = "FAIL";
    /** 调用状态 - 超时 */
    public static final String CALL_STATUS_TIMEOUT = "TIMEOUT";

    /** 适配器类型 - REST API */
    public static final String ADAPTER_REST_API = "REST_API";
    /** 适配器类型 - Webhook */
    public static final String ADAPTER_WEBHOOK = "WEBHOOK";
    /** 适配器类型 - 数据库 */
    public static final String ADAPTER_DATABASE = "DATABASE";
    /** 适配器类型 - 消息队列 */
    public static final String ADAPTER_MESSAGE_QUEUE = "MESSAGE_QUEUE";

    /** 认证方式 - 无 */
    public static final String AUTH_NONE = "NONE";
    /** 认证方式 - Basic */
    public static final String AUTH_BASIC = "BASIC";
    /** 认证方式 - Bearer Token */
    public static final String AUTH_BEARER = "BEARER";
    /** 认证方式 - API Key */
    public static final String AUTH_API_KEY = "API_KEY";
    /** 认证方式 - OAuth2 */
    public static final String AUTH_OAUTH2 = "OAUTH2";
}
