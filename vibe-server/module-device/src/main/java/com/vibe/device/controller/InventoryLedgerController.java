package com.vibe.device.controller;

import com.vibe.common.result.Result;
import com.vibe.device.service.DeviceInventoryService;
import com.vibe.device.vo.InventoryLedgerRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存台账 Controller
 *
 * <p>各仓库各型号在库（IN_FACTORY）设备数量聚合查询，用于库存盘点看板。</p>
 *
 * @author vibe
 */
@Tag(name = "库存台账", description = "各仓库各型号在库设备数量聚合")
@RestController
@RequestMapping("/api/v1/devices/inventory/ledger")
@RequiredArgsConstructor
public class InventoryLedgerController {

    private final DeviceInventoryService deviceInventoryService;

    @Operation(summary = "库存台账聚合查询（各仓库各型号在库数量）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping
    public Result<List<InventoryLedgerRow>> ledger(@RequestParam(required = false) Long warehouseId,
                                                   @RequestParam(required = false) Long modelId) {
        return Result.success(deviceInventoryService.ledger(warehouseId, modelId));
    }
}
