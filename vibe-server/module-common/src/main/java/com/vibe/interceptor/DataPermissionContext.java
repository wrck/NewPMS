package com.vibe.interceptor;

import com.vibe.annotation.DataPermission;

/**
 * 数据权限上下文（ThreadLocal）
 *
 * <p>用于在 Service 方法上标注 {@link DataPermission} 时，
 * 通过 {@link DataPermissionAspect} AOP 切面将注解信息传递给 MyBatis 拦截器
 * ({@code DataPermissionInnerInterceptor})。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>Service 方法被调用 → {@code DataPermissionAspect} 拦截，
 *       将方法上的 {@link DataPermission} 注解写入 ThreadLocal</li>
 *   <li>Service 内部调用 Mapper → MyBatis 拦截器执行 {@code beforeQuery}，
 *       优先从本 ThreadLocal 读取注解</li>
 *   <li>Service 方法返回 → 切面 finally 块清理 ThreadLocal</li>
 * </ol>
 *
 * <p>Mapper 方法上的注解由拦截器直接通过反射获取，无需此上下文。</p>
 *
 * @author vibe
 */
public final class DataPermissionContext {

    private static final ThreadLocal<DataPermission> HOLDER = new ThreadLocal<>();

    private DataPermissionContext() {
    }

    /**
     * 设置当前线程的数据权限注解。
     */
    public static void set(DataPermission annotation) {
        HOLDER.set(annotation);
    }

    /**
     * 获取当前线程的数据权限注解。
     *
     * @return 注解实例，未设置时返回 null
     */
    public static DataPermission get() {
        return HOLDER.get();
    }

    /**
     * 清理当前线程的数据权限注解，避免内存泄漏。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
