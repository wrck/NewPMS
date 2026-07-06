package com.vibe.integration.controller;

import com.vibe.integration.adapter.erp.ErpCustomerSyncService;
import com.vibe.integration.adapter.erp.dto.ErpCustomerDTO;
import com.vibe.integration.adapter.logistics.LogisticsStatusService;
import com.vibe.integration.adapter.logistics.dto.LogisticsStatusDTO;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 集成同步 Controller - 手动触发外部系统同步端点
 *
 * <p>路径前缀：{@code /api/v1/integration}</p>
 *
 * <p>提供运维/管理员手动触发 ERP 客户主数据同步、物流状态拉取的入口，
 * 用于排查同步异常、补齐漏同步数据。日常定时同步建议通过 XXL-JOB 调度触发（Task 2）。</p>
 *
 * @author vibe
 */
@Tag(name = "集成同步", description = "手动触发外部系统同步（ERP/物流）")
@RestController
@RequestMapping("/api/v1/integration")
@RequiredArgsConstructor
public class IntegrationSyncController {

    private final ErpCustomerSyncService erpCustomerSyncService;
    private final LogisticsStatusService logisticsStatusService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* ============ ERP 同步 ============ */

    @Operation(summary = "手动触发 ERP 全量/增量客户同步")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/erp/sync")
    public Result<List<ErpCustomerDTO>> syncErpCustomers(
            @Parameter(description = "增量同步起始时间（yyyy-MM-dd HH:mm:ss），为空则全量同步")
            @RequestParam(value = "updatedAfter", required = false) String updatedAfter) {
        LocalDateTime after = null;
        if (updatedAfter != null && !updatedAfter.isBlank()) {
            after = LocalDateTime.parse(updatedAfter, DATE_FMT);
        }
        List<ErpCustomerDTO> result = erpCustomerSyncService.syncAllCustomers(after);
        return Result.success(result);
    }

    @Operation(summary = "同步单个 ERP 客户")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/erp/sync/customer")
    public Result<ErpCustomerDTO> syncErpCustomer(
            @Parameter(description = "ERP 客户主键", required = true)
            @RequestParam Long customerId) {
        return Result.success(erpCustomerSyncService.syncCustomer(customerId));
    }

    /* ============ 物流状态同步 ============ */

    @Operation(summary = "手动触发物流状态拉取（按运单号）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/logistics/sync")
    public Result<LogisticsStatusDTO> pullLogisticsStatus(
            @Parameter(description = "运单号", required = true)
            @RequestParam String trackingNo) {
        return Result.success(logisticsStatusService.pullLogisticsStatus(trackingNo));
    }

    @Operation(summary = "批量拉取物流状态")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/logistics/sync/batch")
    public Result<List<LogisticsStatusDTO>> batchPullLogisticsStatus(
            @RequestBody List<String> trackingNos) {
        return Result.success(logisticsStatusService.batchPullStatus(trackingNos));
    }
}
