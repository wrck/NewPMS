package com.vibe.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * <p>标注于 Controller 方法上，AOP 切面捕获后写入 sys_log 操作日志表。</p>
 *
 * @author vibe
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型，如 CREATE/UPDATE/DELETE/IMPORT/EXPORT/LOGIN 等
     */
    String type() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否记录请求参数
     */
    boolean saveRequest() default true;

    /**
     * 是否记录响应结果
     */
    boolean saveResponse() default false;
}
