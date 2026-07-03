package com.vibe.interceptor;

import com.vibe.annotation.DataPermission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据权限注解 AOP 切面
 *
 * <p>拦截 Service 方法上的 {@link DataPermission} 注解，将注解信息通过
 * {@link DataPermissionContext}（ThreadLocal）传递给 MyBatis 拦截器
 * ({@code DataPermissionInnerInterceptor})。</p>
 *
 * <p>背景：Mapper 接口方法由 MyBatis 动态代理实现，Spring AOP 切面无法直接拦截。
 * 因此 Service 方法上的 {@link DataPermission} 通过本切面把注解放到 ThreadLocal，
 * MyBatis 拦截器在 {@code beforeQuery} 时优先读取 ThreadLocal。
 * Mapper 方法上的注解则由拦截器直接通过反射读取。</p>
 *
 * <p>支持嵌套调用：内层 Service 方法返回后恢复外层注解，保证嵌套场景下数据权限正确传递。</p>
 *
 * <p>切面优先级：使用 {@link Order} 指定为较高优先级，确保在事务切面之前执行，
 * 使得数据权限上下文在事务内的所有 Mapper 调用中均可用。</p>
 *
 * @author vibe
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class DataPermissionAspect {

    /**
     * 环绕通知：拦截所有标注 {@link DataPermission} 的方法。
     *
     * <p>把注解放入 ThreadLocal 后执行原方法，方法返回后恢复外层注解（嵌套场景）
     * 或清理 ThreadLocal，避免内存泄漏。</p>
     *
     * @param joinPoint       切点
     * @param dataPermission  方法上的数据权限注解（由 Spring AOP 自动注入）
     * @return 原方法返回值
     * @throws Throwable 原方法抛出的异常
     */
    @Around("@annotation(dataPermission)")
    public Object around(ProceedingJoinPoint joinPoint, DataPermission dataPermission) throws Throwable {
        // 保存外层数据权限上下文（嵌套调用场景）
        DataPermission previous = DataPermissionContext.get();
        try {
            DataPermissionContext.set(dataPermission);
            return joinPoint.proceed();
        } finally {
            // 恢复外层或清理
            if (previous != null) {
                DataPermissionContext.set(previous);
            } else {
                DataPermissionContext.clear();
            }
        }
    }
}
