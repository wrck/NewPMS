package com.vibe.common.result;

import lombok.Getter;

/**
 * 系统统一错误码枚举
 *
 * <p>错误码范围规范：</p>
 * <ul>
 *   <li>200           - 成功</li>
 *   <li>400xx (40000-40099) - 参数校验错误</li>
 *   <li>401xx (40100-40199) - 认证错误（未登录/Token过期）</li>
 *   <li>403xx (40300-40399) - 权限不足</li>
 *   <li>404xx (40400-40499) - 资源不存在</li>
 *   <li>409xx (40900-40999) - 业务冲突（状态不允许/重复操作）</li>
 *   <li>500xx (50000-50099) - 系统内部错误</li>
 *   <li>502xx (50200-50299) - 外部服务调用失败</li>
 * </ul>
 *
 * @author vibe
 */
@Getter
public enum ResultCode {

    /* ============ 成功 ============ */
    SUCCESS(200, "success"),

    /* ============ 400xx 参数校验错误 ============ */
    PARAM_INVALID(40000, "参数校验失败"),
    PARAM_MISSING(40001, "参数缺失"),
    PARAM_TYPE_MISMATCH(40002, "参数类型不匹配"),
    PARAM_BIND_ERROR(40003, "参数绑定错误"),
    JSON_PARSE_ERROR(40004, "JSON 解析错误"),
    HTTP_MESSAGE_NOT_READABLE(40005, "请求体格式错误"),

    /* ============ 401xx 认证错误 ============ */
    UNAUTHORIZED(40100, "未登录或登录已失效"),
    TOKEN_INVALID(40101, "Token 无效"),
    TOKEN_EXPIRED(40102, "Token 已过期"),
    TOKEN_BLACKLISTED(40103, "Token 已被加入黑名单"),
    ACCOUNT_DISABLED(40104, "账号已被禁用"),
    ACCOUNT_LOCKED(40105, "账号已被锁定"),
    ACCOUNT_NOT_FOUND(40106, "账号不存在"),
    PASSWORD_ERROR(40107, "账号或密码错误"),
    CAPTCHA_ERROR(40108, "验证码错误"),
    SMS_CODE_ERROR(40109, "短信验证码错误"),

    /* ============ 403xx 权限不足 ============ */
    FORBIDDEN(40300, "权限不足"),
    NO_PERMISSION(40301, "无操作权限"),
    DATA_PERMISSION_DENIED(40302, "无数据访问权限"),

    /* ============ 404xx 资源不存在 ============ */
    NOT_FOUND(40400, "资源不存在"),
    PROJECT_NOT_FOUND(40401, "项目不存在"),
    DEVICE_NOT_FOUND(40402, "设备不存在"),
    TASK_NOT_FOUND(40403, "任务不存在"),
    USER_NOT_FOUND(40404, "用户不存在"),
    WORK_ORDER_NOT_FOUND(40405, "工单不存在"),
    AGENT_NOT_FOUND(40406, "代理商不存在"),
    OUTSOURCE_TASK_NOT_FOUND(40407, "转包任务不存在"),
    FILE_NOT_FOUND(40408, "文件不存在"),

    /* ============ 409xx 业务冲突 ============ */
    BUSINESS_CONFLICT(40900, "业务冲突"),
    STATE_NOT_ALLOWED(40901, "当前状态不允许此操作"),
    STATE_TRANSITION_INVALID(40902, "状态流转非法"),
    DUPLICATE_OPERATION(40903, "重复操作"),
    DATA_DUPLICATED(40904, "数据重复"),
    SN_DUPLICATED(40905, "设备 SN 已存在"),
    PROJECT_CODE_DUPLICATED(40906, "项目编号已存在"),
    SCHEDULE_CONFLICT(40907, "排期冲突"),
    DEPENDENCY_CONFLICT(40908, "任务依赖冲突"),
    BALANCE_INSUFFICIENT(40909, "余额不足"),
    STOCK_INSUFFICIENT(40910, "库存不足"),

    /* ============ 500xx 系统内部错误 ============ */
    INTERNAL_ERROR(50000, "系统内部错误"),
    DB_ERROR(50001, "数据库错误"),
    CACHE_ERROR(50002, "缓存错误"),
    FILE_IO_ERROR(50003, "文件读写错误"),
    NETWORK_ERROR(50004, "网络错误"),
    CONFIG_ERROR(50005, "系统配置错误"),
    UNKNOWN_ERROR(50099, "未知系统错误"),

    /* ============ 502xx 外部服务调用失败 ============ */
    EXTERNAL_SERVICE_ERROR(50200, "外部服务调用失败"),
    SMS_SEND_FAILED(50201, "短信发送失败"),
    EMAIL_SEND_FAILED(50202, "邮件发送失败"),
    IM_PUSH_FAILED(50203, "IM 消息推送失败"),
    MAP_SERVICE_ERROR(50204, "地图服务调用失败"),
    ERP_SYNC_FAILED(50205, "ERP 同步失败"),
    NMS_SYNC_FAILED(50206, "网管同步失败"),
    LOGISTICS_QUERY_FAILED(50207, "物流查询失败"),
    MINIO_ERROR(50208, "MinIO 文件服务异常");

    /** 错误码 */
    private final int code;

    /** 错误消息 */
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据错误码获取枚举
     */
    public static ResultCode getByCode(int code) {
        for (ResultCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        return null;
    }
}
