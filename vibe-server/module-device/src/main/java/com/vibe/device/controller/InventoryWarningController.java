package com.vibe.device.controller;

import com.vibe.common.result.Result;
import com.vibe.device.service.DeviceInventoryService;
import com.vibe.device.vo.InventoryWarningVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存预警 Controller
 *
 * <p>扫描所有仓库，比对在库数量与 safety_stock 配置，
 * 返回在库数量低于安全库存阈值的清单。</p>
 *
 * @author vibe
 */
@Tag(name = "库存预警", description = "低于安全库存告警")
@RestController
@RequestMapping("/api/v1/devices/inventory/warnings")
@RequiredArgsConstructor
public class InventoryWarningController {

    private final DeviceInventoryService deviceInventoryService;

    @Operation(summary = "库存预警查询（在库数量低于安全库存的仓库+型号清单）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping
    public Result<List<InventoryWarningVO>> warnings() {
        return Result.success(deviceInventoryService.warnings());
    }
}
