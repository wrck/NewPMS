package com.vibe.auth.domain.enums;

/**
 * 用户类型枚举（多类型用户认证体系）
 *
 * <p>区分内部用户、代理商、客户三类独立认证体系，不同类型配置不同的
 * Token 有效期与权限边界。</p>
 *
 * <ul>
 *   <li>{@link #INTERNAL} —— 内部用户（PC 端账号密码登录，Token 8h）</li>
 *   <li>{@link #AGENT} —— 代理商工程师（H5 手机号验证码登录，Token 7d）</li>
 *   <li>{@link #CUSTOMER} —— 客户（H5 手机号验证码登录，Token 2h）</li>
 * </ul>
 *
 * @author vibe
 */
public enum UserType {

    /** 内部用户 */
    INTERNAL,
    /** 代理商工程师 */
    AGENT,
    /** 客户 */
    CUSTOMER;

    /**
     * 从字符串安全解析 UserType，解析失败返回 null。
     *
     * @param value 字符串值（大小写不敏感）
     * @return UserType 实例，或 null
     */
    public static UserType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UserType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
