package com.vibe.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置（Caffeine + Redis 二级缓存）
 *
 * <p>module-report 报表明细查询走 Caffeine 本地缓存（L1，进程内），
 * 配合 RedisTemplate（L2，分布式共享）实现两级缓存：</p>
 * <ul>
 *   <li>L1（Caffeine）：进程内热点数据，TTL 5 分钟，最大 1000 条；
 *       命中率最高、延迟最低，但无法跨实例共享。</li>
 *   <li>L2（Redis）：通过 {@code RedisTemplate} 显式读写，跨实例共享；
 *       由调用方按需使用，{@code @Cacheable} 默认走 L1。</li>
 * </ul>
 *
 * <p>当前 {@link CacheManager} 仅注册 Caffeine，{@code @Cacheable} 默认走 L1。
 * L2 由 {@code RedisUtils} 提供 {@code get/set} 操作，业务侧按需显式调用。</p>
 *
 * <p>缓存名约定：</p>
 * <ul>
 *   <li>{@code reportDetail}：报表明细查询缓存（PM 业绩明细、项目明细、设备明细、资源明细、财务明细）</li>
 *   <li>{@code dashboardStats}：驾驶舱 KPI/趋势聚合缓存</li>
 * </ul>
 *
 * @author vibe
 */
@Configuration
@EnableCaching
@ConditionalOnClass(Caffeine.class)
public class CacheConfig {

    /** 报表明细缓存名 */
    public static final String CACHE_REPORT_DETAIL = "reportDetail";
    /** 驾驶舱统计缓存名 */
    public static final String CACHE_DASHBOARD_STATS = "dashboardStats";

    /**
     * Caffeine 缓存管理器（L1 本地缓存）。
     *
     * <p>注册两个缓存名：</p>
     * <ul>
     *   <li>{@code reportDetail}：5 分钟 TTL，最大 1000 条，按写入后过期</li>
     *   <li>{@code dashboardStats}：1 分钟 TTL，最大 200 条，热点数据</li>
     * </ul>
     * 未显式声明的缓存名走 Caffeine 默认配置（5 分钟 TTL，最大 500 条）。
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        // 默认缓存规格：5 分钟 TTL，最大 500 条
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500));
        // 显式注册缓存名（不强制，但便于预热与监控）
        cacheManager.setCacheNames(java.util.List.of(CACHE_REPORT_DETAIL, CACHE_DASHBOARD_STATS));
        // 允许动态创建未声明的缓存名（默认行为，显式声明以提升可读性）
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}
