package com.vibe.interceptor;

import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文清理拦截器
 *
 * <p>在请求结束时清理 ThreadLocal，避免内存泄漏。
 * 实际写入用户上下文由 module-auth 的 JWT 过滤器完成。</p>
 *
 * <p>注册方式：WebMvcConfigurer#addInterceptors</p>
 *
 * @author vibe
 */
public class UserContextCleanupInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
