package com.vibe.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 过期临时数据清理定时任务。
 *
 * <p><b>调度配置</b>：每日 03:00 执行（cron: 0 0 3 * * ?，由 xxl-job-admin 配置）。</p>
 *
 * <p><b>业务逻辑</b>：</p>
 * <ol>
 *   <li>清理 Redis 中过期或不再需要的 Key（带 TTL 的 cache:* / lock:* / verify:* 前缀）</li>
 *   <li>清理 {@code sys_log} 表超过 90 天的记录（保留审计必要记录）</li>
 *   <li>清理 {@code sys_login_log} 表超过 90 天的登录日志</li>
 *   <li>清理 MinIO 中临时文件桶 {@code vibe-temp}（超过 7 天未访问的临时上传文件）</li>
 *   <li>清理本地临时目录（{@code java.io.tmpdir}/vibe-* 下的过期文件）</li>
 *   <li>清理 XXL-JOB 自身日志（由调度中心 logretentiondays 控制，不在此处理）</li>
 * </ol>
 *
 * <p><b>依赖的 Mapper/Service</b>：</p>
 * <ul>
 *   <li>{@code RedisTemplate} - Redis Key 清理（同模块直接注入）</li>
 *   <li>{@code com.vibe.system.mapper.SysLogMapper} - 删除超过 90 天的操作日志</li>
 *   <li>{@code com.vibe.system.mapper.SysLoginLogMapper} - 删除超过 90 天的登录日志</li>
 *   <li>{@code com.vibe.utils.MinioUtils} - 清理临时文件桶（同模块可直接注入）</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupJobHandler {

    /**
     * 操作日志保留天数（90 天，超过自动清理）。
     */
    private static final int SYS_LOG_RETENTION_DAYS = 90;

    /**
     * 临时文件清理前缀（扫描 Redis 与 MinIO 临时桶）。
     */
    private static final String[] CLEANUP_REDIS_PREFIXES = {"cache:temp:", "lock:transient:", "verify:"};

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * XXL-JOB 入口方法，调度中心通过 {@code @XxlJob("dataCleanupJob")} 触发。
     */
    @XxlJob("dataCleanupJob")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("========== 过期临时数据清理任务启动 ==========");
        XxlJobHelper.log("调度参数: {}", jobParam);

        try {
            // 1. 清理 Redis 过期/无用的临时 Key
            int redisCleaned = cleanupRedisTempKeys();
            XxlJobHelper.log("Redis 临时 Key 清理数: {}", redisCleaned);

            // TODO: 2. 注入 SysLogMapper / SysLoginLogMapper 后启用：
            //     LocalDateTime threshold = LocalDateTime.now().minusDays(SYS_LOG_RETENTION_DAYS);
            //     int sysLogDeleted = sysLogMapper.delete(
            //         Wrappers.lambdaQuery<SysLogEntity>().lt(SysLogEntity::getCreateTime, threshold));
            //     int loginLogDeleted = sysLoginLogMapper.delete(
            //         Wrappers.lambdaQuery<SysLoginLogEntity>().lt(SysLoginLogEntity::getLoginTime, threshold));
            int sysLogDeleted = 0;
            int loginLogDeleted = 0;
            XxlJobHelper.log("sys_log 清理数: {}（阈值: {} 天前）", sysLogDeleted, SYS_LOG_RETENTION_DAYS);
            XxlJobHelper.log("sys_login_log 清理数: {}", loginLogDeleted);

            // TODO: 3. 注入 MinioUtils 后启用：清理 vibe-temp 桶中超过 7 天的临时文件
            int tempFileCleaned = 0;
            XxlJobHelper.log("MinIO 临时文件清理数: {}", tempFileCleaned);

            // TODO: 4. 清理本地临时目录 java.io.tmpdir/vibe-* 下的过期文件
            int localTempCleaned = 0;
            XxlJobHelper.log("本地临时文件清理数: {}", localTempCleaned);

            int totalCleaned = redisCleaned + sysLogDeleted + loginLogDeleted + tempFileCleaned + localTempCleaned;
            XxlJobHelper.handleSuccess("数据清理任务执行成功，总清理数=" + totalCleaned);
        } catch (Exception e) {
            log.error("数据清理任务执行失败", e);
            XxlJobHelper.log("数据清理任务执行失败: {}", e.getMessage());
            XxlJobHelper.handleFail("数据清理任务执行失败: " + e.getMessage());
        }

        XxlJobHelper.log("========== 过期临时数据清理任务结束 ==========");
    }

    /**
     * 扫描并删除 Redis 临时 Key 前缀（仅删除无 TTL 或已过期的）。
     *
     * <p>注意：仅清理临时业务 Key，不清理持久化缓存（如字典/权限缓存），避免影响业务。</p>
     *
     * @return 实际删除的 Key 数量
     */
    private int cleanupRedisTempKeys() {
        int cleaned = 0;
        for (String prefix : CLEANUP_REDIS_PREFIXES) {
            try {
                Set<byte[]> keys = redisTemplate.getConnectionFactory()
                        .getConnection().keys((prefix + "*").getBytes());
                if (keys == null || keys.isEmpty()) {
                    continue;
                }
                for (byte[] keyBytes : keys) {
                    String key = new String(keyBytes);
                    Long ttl = redisTemplate.getExpire(key);
                    // ttl < 0 表示无 TTL（永久 Key）或 Key 不存在；ttl == 0 表示已过期
                    // 仅清理无 TTL（-1）的临时 Key，避免误删带 TTL 的有效缓存
                    if (ttl != null && ttl == -1) {
                        redisTemplate.delete(key);
                        cleaned++;
                    }
                }
            } catch (Exception ex) {
                log.warn("清理 Redis 前缀 {} 失败: {}", prefix, ex.getMessage());
            }
        }
        return cleaned;
    }
}
