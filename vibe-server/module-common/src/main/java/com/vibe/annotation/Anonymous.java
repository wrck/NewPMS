package com.vibe.annotation;

import java.lang.annotation.*;

/**
 * 接口不需要登录注解
 *
 * <p>标注于 Controller 方法或类上，表示该接口跳过 JWT 校验，
 * 用于登录、注册、验证码等公开接口。</p>
 *
 * <p>注意：实际放行规则在 SecurityConfig 中配置；此注解主要作为元数据标记，
 * 便于扫描统计与文档生成。</p>
 *
 * @author vibe
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous {
}
