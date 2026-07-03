package com.vibe.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.annotation.OperationLog;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.system.entity.SysLogEntity;
import com.vibe.system.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 *
 * <p>拦截 Controller 方法上标注的 {@link OperationLog} 注解，采集请求信息
 * （URL、HTTP 方法、请求参数、客户端 IP、操作人）与响应结果，
 * 异步写入 {@code sys_log} 操作日志表。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>使用 {@code @Around} 环绕通知，可在方法执行前后分别采集请求与响应</li>
 *   <li>日志持久化通过 {@link SysLogService#asyncSave} 异步完成，不阻塞业务</li>
 *   <li>请求参数 / 响应结果通过 Jackson 序列化为 JSON，超长截断</li>
 *   <li>即使日志记录失败也不影响业务流程（catch 后仅打印 warn 日志）</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysLogService sysLogService;
    private final ObjectMapper objectMapper;

    /** 请求参数 / 响应结果最大记录长度，超出截断 */
    private static final int MAX_PAYLOAD_LENGTH = 2000;

    /**
     * 环绕通知：拦截所有标注 {@link OperationLog} 的方法
     *
     * @param joinPoint    切点
     * @param operationLog 方法上的操作日志注解（由 Spring AOP 自动注入）
     * @return 原方法返回值
     * @throws Throwable 原方法抛出的异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startMs = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                buildAndSaveLog(joinPoint, operationLog, result, error, startMs);
            } catch (Exception e) {
                log.warn("[操作日志] 切面记录失败，不影响业务: {}", e.getMessage());
            }
        }
    }

    /**
     * 组装操作日志实体并异步保存
     */
    private void buildAndSaveLog(ProceedingJoinPoint joinPoint, OperationLog ann,
                                 Object result, Throwable error, long startMs) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        SysLogEntity entity = new SysLogEntity();
        entity.setTitle(ann.module());
        entity.setBusinessType(ann.type());
        entity.setMethod(joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName());
        entity.setOperTime(LocalDateTime.now());
        entity.setResponseResult(null);

        // 操作人
        UserContext ctx = UserContextHolder.get();
        if (ctx != null) {
            entity.setOperatorId(ctx.getUserId());
        }

        // 请求信息
        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setRequestUrl(request.getRequestURI());
            entity.setOperIp(getClientIp(request));
        }

        // 请求参数
        if (ann.saveRequest()) {
            entity.setRequestParam(serializeArgs(joinPoint.getArgs()));
        }

        // 响应结果（仅当方法正常返回且开启记录时）
        if (ann.saveResponse() && error == null && result != null) {
            entity.setResponseResult(serialize(result));
        }

        // 描述补充（含异常信息）
        if (error != null) {
            String desc = ann.description();
            String errMsg = error.getMessage();
            if (desc == null || desc.isBlank()) {
                entity.setTitle(ann.module() + "(异常)");
            }
            entity.setResponseResult(truncate("ERROR: " + (errMsg == null ? error.getClass().getSimpleName() : errMsg)));
        }

        long cost = System.currentTimeMillis() - startMs;
        log.debug("[操作日志] {} | {} | {}ms | {}", ann.module(), ann.type(), cost, ann.description());

        sysLogService.asyncSave(entity);
    }

    /**
     * 序列化方法参数为 JSON 字符串（过滤 HttpServletRequest/Response 等不可序列化对象）
     */
    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            // 跳过 Servlet 请求/响应对象
            if (arg instanceof HttpServletRequest
                    || arg.getClass().getName().startsWith("jakarta.servlet")
                    || arg.getClass().getName().startsWith("org.springframework.web")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(serialize(arg));
        }
        return truncate(sb.toString());
    }

    /**
     * 序列化对象为 JSON
     */
    private String serialize(Object obj) {
        try {
            return truncate(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            return truncate(String.valueOf(obj));
        }
    }

    /**
     * 截断超长字符串
     */
    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > MAX_PAYLOAD_LENGTH ? s.substring(0, MAX_PAYLOAD_LENGTH) + "...[truncated]" : s;
    }

    /**
     * 获取当前 HTTP 请求
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /**
     * 获取客户端真实 IP（穿透常见反向代理头）
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // 多级代理时取第一个非 unknown 的 IP
                int comma = ip.indexOf(',');
                return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
