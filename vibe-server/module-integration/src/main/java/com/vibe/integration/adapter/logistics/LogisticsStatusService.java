package com.vibe.integration.adapter.logistics;

import com.vibe.integration.adapter.logistics.dto.LogisticsStatusDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 物流状态拉取 Service
 *
 * <p>调用 {@link LogisticsStatusFeignClient} 拉取物流状态，更新设备 SHIPPED 阶段的预计到货时间。</p>
 *
 * <p>通过 {@code @CircuitBreaker(name = "logistics-adapter")} + {@code @Retry(name = "logistics-adapter")}
 * 实现 Resilience4j 熔断与重试：50% 失败率熔断，10 分钟半开试探，最多 3 次重试。</p>
 *
 * <p>当 {@code integration.logistics.enabled=false}（默认）时所有拉取方法返回降级数据。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsStatusService {

    private final LogisticsStatusFeignClient logisticsStatusFeignClient;

    @Value("${integration.logistics.enabled:false}")
    private boolean logisticsEnabled;

    /**
     * 拉取单个运单的物流状态。
     *
     * <p>熔断开启时返回 {@code null}。</p>
     *
     * @param trackingNo 运单号
     * @return 物流状态；未启用或熔断时返回 {@code null}
     */
    @CircuitBreaker(name = "logistics-adapter", fallbackMethod = "pullLogisticsStatusFallback")
    @Retry(name = "logistics-adapter")
    public LogisticsStatusDTO pullLogisticsStatus(String trackingNo) {
        if (!logisticsEnabled) {
            log.warn("物流状态拉取未启用（integration.logistics.enabled=false），跳过 pullLogisticsStatus({})", trackingNo);
            return null;
        }
        log.info("拉取物流状态 trackingNo={}", trackingNo);
        LogisticsStatusDTO dto = logisticsStatusFeignClient.pullStatus(trackingNo);
        if (dto != null) {
            dto.setPulledAt(LocalDateTime.now());
        }
        return dto;
    }

    /**
     * 批量拉取运单的物流状态。
     *
     * @param trackingNos 运单号列表
     * @return 物流状态列表；未启用或熔断时返回空列表
     */
    @CircuitBreaker(name = "logistics-adapter", fallbackMethod = "batchPullStatusFallback")
    @Retry(name = "logistics-adapter")
    public List<LogisticsStatusDTO> batchPullStatus(List<String> trackingNos) {
        if (!logisticsEnabled) {
            log.warn("物流状态拉取未启用（integration.logistics.enabled=false），跳过 batchPullStatus size={}",
                    trackingNos == null ? 0 : trackingNos.size());
            return Collections.emptyList();
        }
        if (trackingNos == null || trackingNos.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("批量拉取物流状态 size={}", trackingNos.size());
        List<LogisticsStatusDTO> result = logisticsStatusFeignClient.batchPullStatus(trackingNos);
        if (result != null) {
            LocalDateTime now = LocalDateTime.now();
            for (LogisticsStatusDTO dto : result) {
                dto.setPulledAt(now);
            }
        }
        return result;
    }

    /* ============ 降级方法 ============ */

    @SuppressWarnings("unused")
    private LogisticsStatusDTO pullLogisticsStatusFallback(String trackingNo, Throwable t) {
        log.error("物流状态拉取降级触发 trackingNo={} cause={}", trackingNo, t.getMessage());
        return null;
    }

    @SuppressWarnings("unused")
    private List<LogisticsStatusDTO> batchPullStatusFallback(List<String> trackingNos, Throwable t) {
        log.error("物流状态批量拉取降级触发 size={} cause={}",
                trackingNos == null ? 0 : trackingNos.size(), t.getMessage());
        return Collections.emptyList();
    }
}
