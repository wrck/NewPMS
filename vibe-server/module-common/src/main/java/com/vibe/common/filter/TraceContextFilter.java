package com.vibe.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪上下文兜底 Filter
 *
 * <p>在请求入口向 MDC 注入 {@code traceId}，确保下游日志与响应体能携带链路 ID。</p>
 *
 * <h3>与 Micrometer Tracing 的关系</h3>
 * <ul>
 *   <li>启用 Micrometer Tracing（{@code micrometer-tracing-bridge-brave}）时，
 *       Tracing 自动将 {@code traceId}/{@code spanId} 注入 MDC，本 Filter 检测到 MDC 已有
 *       traceId 时不覆盖，仅作透传；同时支持从上游传递的 {@code X-B3-TraceId} 头还原上下文。</li>
 *   <li>未启用 Micrometer Tracing（如单元测试 / 局部环境）时，本 Filter 兜底生成 traceId，
 *       保证 {@link com.vibe.common.result.Result#getTraceId()} 与日志始终可读。</li>
 * </ul>
 *
 * <h3>TraceId 来源优先级</h3>
 * <ol>
 *   <li>MDC 中已存在的 traceId（Micrometer Tracing 已注入）</li>
 *   <li>请求头 {@code X-B3-TraceId}（上游服务传递，Brave / Zipkin B3 协议）</li>
 *   <li>请求头 {@code traceId}（自定义传递协议）</li>
 *   <li>新生成的 16 位十六进制 traceId</li>
 * </ol>
 *
 * <p>使用 {@code HIGHEST_PRECEDENCE} 保证最早执行、最晚清理，覆盖所有后续日志输出。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextFilter implements Filter {

    /** MDC 中 traceId 的 key（与 Micrometer Tracing 默认值对齐） */
    public static final String MDC_TRACE_ID_KEY = "traceId";

    /** MDC 中 spanId 的 key（与 Micrometer Tracing 默认值对齐） */
    public static final String MDC_SPAN_ID_KEY = "spanId";

    /** B3 协议上游传递 traceId 的标准请求头 */
    private static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";

    /** 自定义上游传递 traceId 的请求头（兼容内部链路） */
    private static final String TRACE_ID_HEADER = "traceId";

    /** 标准 traceId 长度（Brave / W3C 兼容，16 位十六进制） */
    private static final int TRACE_ID_LENGTH = 16;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        boolean traceIdInjectedByThisFilter = false;
        try {
            // 优先级 1：Micrometer Tracing 已注入 MDC
            String existingTraceId = MDC.get(MDC_TRACE_ID_KEY);
            if (StringUtils.hasText(existingTraceId)) {
                // Micrometer Tracing 已就位，仅作透传
                if (log.isDebugEnabled()) {
                    log.debug("[TraceContext] 使用 Micrometer Tracing 注入的 traceId={}", existingTraceId);
                }
            } else if (request instanceof HttpServletRequest httpRequest) {
                // 优先级 2/3：从上游请求头恢复 traceId
                String traceId = resolveTraceIdFromHeader(httpRequest);
                if (StringUtils.hasText(traceId)) {
                    MDC.put(MDC_TRACE_ID_KEY, traceId);
                    traceIdInjectedByThisFilter = true;
                    if (log.isDebugEnabled()) {
                        log.debug("[TraceContext] 兜底注入 traceId={} (来源: 请求头)", traceId);
                    }
                } else {
                    // 优先级 4：生成新 traceId
                    String newTraceId = generateTraceId();
                    MDC.put(MDC_TRACE_ID_KEY, newTraceId);
                    traceIdInjectedByThisFilter = true;
                    if (log.isDebugEnabled()) {
                        log.debug("[TraceContext] 兜底生成新 traceId={}", newTraceId);
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            // 仅清理本 Filter 注入的 MDC，避免误清 Micrometer Tracing 后续 span 关联
            if (traceIdInjectedByThisFilter) {
                MDC.remove(MDC_TRACE_ID_KEY);
                MDC.remove(MDC_SPAN_ID_KEY);
            }
        }
    }

    /**
     * 从请求头解析 traceId，优先 B3 协议头，其次自定义头。
     */
    private String resolveTraceIdFromHeader(HttpServletRequest request) {
        String b3TraceId = request.getHeader(B3_TRACE_ID_HEADER);
        if (StringUtils.hasText(b3TraceId)) {
            return normalizeTraceId(b3TraceId);
        }
        String customTraceId = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.hasText(customTraceId)) {
            return normalizeTraceId(customTraceId);
        }
        return null;
    }

    /**
     * 规范化 traceId：截断/补齐到标准长度（16 位十六进制小写）。
     */
    private String normalizeTraceId(String raw) {
        String normalized = raw.trim().toLowerCase();
        if (normalized.length() == TRACE_ID_LENGTH) {
            return normalized;
        }
        if (normalized.length() > TRACE_ID_LENGTH) {
            // B3 协议 32 位 traceId，取后 16 位（与 Brave 兼容策略一致）
            return normalized.substring(normalized.length() - TRACE_ID_LENGTH);
        }
        // 短于 16 位：左侧补 0
        return String.format("%0" + TRACE_ID_LENGTH + "x", Long.parseUnsignedLong(normalized, 16));
    }

    /**
     * 生成 16 位十六进制 traceId（与 Brave TraceId 生成策略兼容）。
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
    }
}
