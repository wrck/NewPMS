package com.vibe.auth.filter;

import com.vibe.auth.provider.JwtTokenProvider;
import com.vibe.common.constant.CommonConstant;
import com.vibe.common.constant.RedisKeyConstant;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.utils.RedisUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器
 *
 * <p>每次请求执行：</p>
 * <ol>
 *   <li>从 Authorization 头提取 Bearer Token</li>
 *   <li>校验 Token 有效性 + 黑名单</li>
 *   <li>解析出 {@link UserContext} 写入 ThreadLocal</li>
 *   <li>构造 Spring Security Authentication 写入 SecurityContext</li>
 *   <li>请求结束时由 {@link com.vibe.interceptor.UserContextCleanupInterceptor} 清理 ThreadLocal</li>
 * </ol>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtils redisUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(CommonConstant.HEADER_AUTHORIZATION);
        String token = JwtTokenProvider.extractToken(authHeader);

        if (StringUtils.hasText(token)) {
            try {
                // 1. 校验 Token 签名/过期
                UserContext ctx = jwtTokenProvider.parseUserContext(token);

                // 2. 校验黑名单（强制下线）
                if (ctx.getTokenId() != null
                        && redisUtils.hasKey(RedisKeyConstant.tokenBlacklist(ctx.getTokenId()))) {
                    log.warn("[JWT] Token 已在黑名单: tokenId={}, userId={}", ctx.getTokenId(), ctx.getUserId());
                    writeUnauthorized(response, "Token 已被加入黑名单，请重新登录");
                    return;
                }

                // 3. 写入 ThreadLocal 上下文
                UserContextHolder.set(ctx);

                // 4. 写入 Spring Security 上下文（角色转 ROLE_ 前缀 + 权限）
                List<SimpleGrantedAuthority> authorities = ctx.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(ctx, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 5. Token 自动续签（剩余 < 2h 时，在响应头返回新 Token）
                if (jwtTokenProvider.needsRenew(token)) {
                    String newToken = jwtTokenProvider.renewToken(token);
                    response.setHeader("X-New-Token", newToken);
                    response.setHeader("Access-Control-Expose-Headers", "X-New-Token");
                }
            } catch (Exception e) {
                log.debug("[JWT] Token 解析失败，按未登录处理: {}", e.getMessage());
                // 不阻断请求链路，让 Security 后续授权规则决定（401 由全局异常处理）
                SecurityContextHolder.clearContext();
                UserContextHolder.clear();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 写 401 响应（Token 黑名单场景直接拒绝）
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":40103,\"message\":\"" + message + "\",\"data\":null,\"timestamp\":"
                        + System.currentTimeMillis() + "}");
    }
}
