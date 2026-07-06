package com.vibe.integration.adapter.erp;

import com.vibe.integration.adapter.erp.dto.ErpCustomerDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ERP 客户主数据同步 Service
 *
 * <p>调用 {@link ErpCustomerFeignClient} 同步 ERP 客户主数据到本系统。</p>
 *
 * <p>通过 {@code @CircuitBreaker(name = "erp-adapter")} + {@code @Retry(name = "erp-adapter")}
 * 实现 Resilience4j 熔断与重试：50% 失败率熔断、10 分钟半开试探、最多 3 次重试（间隔 1s）。</p>
 *
 * <p>当 {@code integration.erp.enabled=false}（默认）时所有同步方法直接返回降级数据，
 * 调用方需自行处理空结果。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErpCustomerSyncService {

    private final ErpCustomerFeignClient erpCustomerFeignClient;

    @Value("${integration.erp.enabled:false}")
    private boolean erpEnabled;

    /**
     * 同步单个客户主数据。
     *
     * <p>熔断开启时返回 {@code null}（降级数据）。</p>
     *
     * @param customerId ERP 客户主键
     * @return 客户主数据；未启用或熔断时返回 {@code null}
     */
    @CircuitBreaker(name = "erp-adapter", fallbackMethod = "syncCustomerFallback")
    @Retry(name = "erp-adapter")
    public ErpCustomerDTO syncCustomer(Long customerId) {
        if (!erpEnabled) {
            log.warn("ERP 同步未启用（integration.erp.enabled=false），跳过 syncCustomer({})", customerId);
            return null;
        }
        log.info("同步 ERP 客户主数据 customerId={}", customerId);
        ErpCustomerDTO dto = erpCustomerFeignClient.getCustomerById(customerId);
        if (dto != null) {
            dto.setSyncedAt(LocalDateTime.now());
        }
        return dto;
    }

    /**
     * 同步全部客户主数据（增量：仅返回 ERP 中 {@code updatedAfter} 之后更新的客户）。
     *
     * @param updatedAfter 增量同步起始时间，{@code null} 表示全量同步
     * @return 客户列表；未启用或熔断时返回空列表
     */
    @CircuitBreaker(name = "erp-adapter", fallbackMethod = "syncAllCustomersFallback")
    @Retry(name = "erp-adapter")
    public List<ErpCustomerDTO> syncAllCustomers(LocalDateTime updatedAfter) {
        if (!erpEnabled) {
            log.warn("ERP 同步未启用（integration.erp.enabled=false），跳过 syncAllCustomers");
            return Collections.emptyList();
        }
        String updatedAfterStr = updatedAfter == null ? null : updatedAfter.toString();
        log.info("增量同步 ERP 客户主数据 updatedAfter={}", updatedAfterStr);

        List<ErpCustomerDTO> result = new ArrayList<>();
        int page = 1;
        int size = 100;
        while (true) {
            List<ErpCustomerDTO> batch = erpCustomerFeignClient.listCustomers(updatedAfterStr, page, size);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            LocalDateTime now = LocalDateTime.now();
            for (ErpCustomerDTO dto : batch) {
                dto.setSyncedAt(now);
            }
            result.addAll(batch);
            if (batch.size() < size) {
                break;
            }
            page++;
        }
        log.info("ERP 客户主数据同步完成，共 {} 条", result.size());
        return result;
    }

    /* ============ 降级方法 ============ */

    /**
     * syncCustomer 熔断/失败降级：返回 {@code null} 表示本次同步未完成，调用方应处理空数据。
     */
    @SuppressWarnings("unused")
    private ErpCustomerDTO syncCustomerFallback(Long customerId, Throwable t) {
        log.error("ERP 客户同步降级触发 customerId={} cause={}", customerId, t.getMessage());
        return null;
    }

    /**
     * syncAllCustomers 熔断/失败降级：返回空列表。
     */
    @SuppressWarnings("unused")
    private List<ErpCustomerDTO> syncAllCustomersFallback(LocalDateTime updatedAfter, Throwable t) {
        log.error("ERP 客户批量同步降级触发 updatedAfter={} cause={}", updatedAfter, t.getMessage());
        return Collections.emptyList();
    }
}
