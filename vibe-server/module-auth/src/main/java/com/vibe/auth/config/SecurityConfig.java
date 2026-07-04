package com.vibe.auth.config;

import com.vibe.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 配置
 *
 * <p>核心策略：</p>
 * <ul>
 *   <li>无状态会话（STATELESS），完全依赖 JWT</li>
 *   <li>放行：登录/登出/刷新 Token、Knife4j 文档、静态资源、OPTIONS 预检</li>
 *   <li>其它请求需经过 {@link JwtAuthenticationFilter} 校验 Token</li>
 *   <li>开启方法级权限注解 {@code @PreAuthorize}（@EnableMethodSecurity）</li>
 *   <li>密码加密：BCrypt</li>
 * </ul>
 *
 * @author vibe
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 放行路径白名单 */
    private static final String[] WHITELIST = {
            // 认证相关
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/auth/captcha/**",
            "/api/v1/auth/sms/**",
            "/api/v1/auth/customer/login",
            // 客户门户 token 访问（仅 GET，无需登录态，token 即为凭证）
            // 注意：POST 提交审批/签核结果必须登录态，不在白名单中
            "/api/v1/customer/cutover/*",
            "/api/v1/customer/acceptance/*",
            // Knife4j / OpenAPI 文档
            "/doc.html",
            "/doc.html/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/v3/api-docs/**",
            "/favicon.ico",
            // 静态资源
            "/static/**",
            "/public/**",
            "/web/**",
            // 健康检查
            "/actuator/**",
            // 错误页
            "/error"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（无状态 JWT 不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 关闭默认登录表单/Basic/Frame
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // CORS 由 WebMvcConfig 提供，这里禁用 Spring Security 的 CORS 配置避免重复
                .cors(AbstractHttpConfigurer::disable)
                // 无状态会话
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                // 异常处理（认证失败、权限不足由全局异常处理器兜底）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, resp, authEx) ->
                                resp.sendError(401, "未登录或登录已失效"))
                        .accessDeniedHandler((req, resp, denied) ->
                                resp.sendError(403, "权限不足"))
                )
                // 注册 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
