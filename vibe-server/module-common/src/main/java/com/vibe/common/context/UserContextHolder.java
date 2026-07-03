package com.vibe.common.context;

/**
 * 用户上下文持有者（ThreadLocal）
 *
 * <p>JWT 过滤器解析出 {@link UserContext} 后调用 {@link #set} 写入，
 * 业务层可通过 {@link #get} 获取当前登录用户信息。
 * 请求结束时必须调用 {@link #clear} 清理，避免内存泄漏。</p>
 *
 * @author vibe
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static UserContext get() {
        return CONTEXT_HOLDER.get();
    }

    public static Long getUserId() {
        UserContext ctx = CONTEXT_HOLDER.get();
        return ctx == null ? null : ctx.getUserId();
    }

    public static String getTenantType() {
        UserContext ctx = CONTEXT_HOLDER.get();
        return ctx == null ? null : ctx.getTenantType();
    }

    public static Long getTenantId() {
        UserContext ctx = CONTEXT_HOLDER.get();
        return ctx == null ? null : ctx.getTenantId();
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
