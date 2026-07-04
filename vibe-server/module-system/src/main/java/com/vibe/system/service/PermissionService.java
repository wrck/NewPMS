package com.vibe.system.service;

import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 权限校验服务（Bean 名称 {@code ss}，供 SpEL 表达式使用：{@code @PreAuthorize("@ss.hasPermi('xxx')")}）。
 *
 * <p>校验策略：</p>
 * <ol>
 *   <li>未登录 -> 返回 false</li>
 *   <li>SUPER_ADMIN 角色 -> 直接放行，避免每次查询数据库</li>
 *   <li>其他用户 -> 查询其权限标识列表（{@link SysMenuService#getPermissionsByUserId}），
 *       命中即放行；权限列表在内存中短时缓存（默认 60s）以降低 DB 压力</li>
 * </ol>
 *
 * <p>用法示例：</p>
 * <pre>
 *   &#64;PreAuthorize("@ss.hasPermi('system:user') or hasRole('SUPER_ADMIN')")
 *   public Result&lt;...&gt; someMethod() { ... }
 * </pre>
 *
 * @author vibe
 */
@Slf4j
@Service("ss")
@RequiredArgsConstructor
public class PermissionService {

    private final SysMenuService sysMenuService;

    /** 简单内存缓存：userId -> (权限集合 + 过期时间)。仅供非超管用户使用 */
    private final ConcurrentHashMap<Long, PermCacheEntry> permCache = new ConcurrentHashMap<>();

    /** 权限缓存 TTL（秒） */
    private static final long CACHE_TTL_SECONDS = 60L;

    /**
     * 校验当前用户是否拥有指定权限标识。
     *
     * @param permission 权限标识，如 {@code system:user}
     * @return true-拥有；false-不拥有或未登录
     */
    public boolean hasPermi(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            return false;
        }
        // 超级管理员直接放行
        if (ctx.isSuperAdmin()) {
            return true;
        }
        Set<String> perms = loadUserPermissions(ctx.getUserId());
        return perms.contains(permission);
    }

    /**
     * 校验当前用户是否拥有任意一个权限。
     */
    public boolean hasAnyPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            return false;
        }
        if (ctx.isSuperAdmin()) {
            return true;
        }
        Set<String> perms = loadUserPermissions(ctx.getUserId());
        for (String p : permissions) {
            if (StringUtils.hasText(p) && perms.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验当前用户是否拥有全部权限。
     */
    public boolean hasAllPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            return false;
        }
        if (ctx.isSuperAdmin()) {
            return true;
        }
        Set<String> perms = loadUserPermissions(ctx.getUserId());
        for (String p : permissions) {
            if (!StringUtils.hasText(p) || !perms.contains(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 加载用户权限集合（带短时内存缓存）。
     */
    private Set<String> loadUserPermissions(Long userId) {
        PermCacheEntry cached = permCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            return cached.perms;
        }
        List<String> permList;
        try {
            permList = sysMenuService.getPermissionsByUserId(userId);
        } catch (Exception e) {
            log.warn("[Permission] 加载用户权限失败 userId={}: {}", userId, e.getMessage());
            return Collections.emptySet();
        }
        if (permList == null) {
            permList = Collections.emptyList();
        }
        Set<String> result = Set.copyOf(permList);
        permCache.put(userId, new PermCacheEntry(result, System.currentTimeMillis() + CACHE_TTL_SECONDS * 1000L));
        return result;
    }

    /** 权限缓存条目 */
    private static class PermCacheEntry {
        final Set<String> perms;
        final long expireAt;

        PermCacheEntry(Set<String> perms, long expireAt) {
            this.perms = perms;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
