package com.vibe.integration.adapter.oa;

import com.vibe.integration.adapter.oa.dto.OaApprovalDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OA 审批联动 Service
 *
 * <p>调用 {@link OaApprovalFeignClient} 联动 OA 系统审批流程
 * （项目立项/验收/割接审批可联动 OA 系统）。</p>
 *
 * <p>通过 {@code @CircuitBreaker(name = "oa-adapter")} + {@code @Retry(name = "oa-adapter")}
 * 实现 Resilience4j 熔断与重试：50% 失败率熔断，10 分钟半开试探，最多 3 次重试。</p>
 *
 * <p>当 {@code integration.oa.enabled=false}（默认）时所有方法返回 {@code null}，调用方应保留本系统 Flowable 流程作为兜底。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaApprovalService {

    private final OaApprovalFeignClient oaApprovalFeignClient;

    @Value("${integration.oa.enabled:false}")
    private boolean oaEnabled;

    /**
     * 启动 OA 审批流程。
     *
     * <p>熔断开启时返回 {@code null}，调用方应仅依赖本系统 Flowable 流程。</p>
     *
     * @param dto OA 审批 DTO
     * @return 启动结果（含 OA 流程实例 ID）；未启用或熔断时返回 {@code null}
     */
    @CircuitBreaker(name = "oa-adapter", fallbackMethod = "startApprovalFallback")
    @Retry(name = "oa-adapter")
    public OaApprovalDTO startApproval(OaApprovalDTO dto) {
        if (!oaEnabled) {
            log.warn("OA 审批联动未启用（integration.oa.enabled=false），跳过 startApproval bizProcessId={}",
                    dto.getBizProcessId());
            return null;
        }
        log.info("启动 OA 审批流程 bizType={} bizProcessId={}", dto.getBizType(), dto.getBizProcessId());
        return oaApprovalFeignClient.startApproval(dto);
    }

    /**
     * 查询 OA 审批流程状态。
     *
     * @param oaProcessId OA 流程实例 ID
     * @return 审批状态详情；未启用或熔断时返回 {@code null}
     */
    @CircuitBreaker(name = "oa-adapter", fallbackMethod = "queryApprovalStatusFallback")
    @Retry(name = "oa-adapter")
    public OaApprovalDTO queryApprovalStatus(String oaProcessId) {
        if (!oaEnabled) {
            log.warn("OA 审批联动未启用（integration.oa.enabled=false），跳过 queryApprovalStatus({})", oaProcessId);
            return null;
        }
        log.info("查询 OA 审批状态 oaProcessId={}", oaProcessId);
        return oaApprovalFeignClient.queryStatus(oaProcessId);
    }

    /* ============ 降级方法 ============ */

    @SuppressWarnings("unused")
    private OaApprovalDTO startApprovalFallback(OaApprovalDTO dto, Throwable t) {
        log.error("OA 审批启动降级触发 bizProcessId={} cause={}", dto.getBizProcessId(), t.getMessage());
        return null;
    }

    @SuppressWarnings("unused")
    private OaApprovalDTO queryApprovalStatusFallback(String oaProcessId, Throwable t) {
        log.error("OA 审批查询降级触发 oaProcessId={} cause={}", oaProcessId, t.getMessage());
        return null;
    }
}
