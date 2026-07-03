package com.vibe.device.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.DeviceInventoryActionDTO;
import com.vibe.device.service.DeviceInventoryService;
import com.vibe.device.vo.DeviceInventoryLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备出入库 Controller
 *
 * <p>设备入库/出库/退库/调拨操作与出入库流水查询。
 * DEVICE_ADMIN/SUPER_ADMIN 可执行操作，PM/ENGINEER 只读流水。</p>
 *
 * @author vibe
 */
@Tag(name = "设备出入库", description = "入库/出库/退库/调拨、流水查询")
@RestController
@RequestMapping("/api/v1/devices/inventory")
@RequiredArgsConstructor
public class DeviceInventoryController {

    private final DeviceInventoryService deviceInventoryService;

    @Operation(summary = "设备出入库操作（入库/出库/退库/调拨）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_INVENTORY, type = "UPDATE", description = "设备出入库操作")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PostMapping("/actions")
    public Result<Void> action(@Valid @RequestBody DeviceInventoryActionDTO dto) {
        deviceInventoryService.action(dto);
        return Result.success();
    }

    @Operation(summary = "出入库流水查询（按仓库/项目/操作类型筛选）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/logs")
    public Result<List<DeviceInventoryLogVO>> logList(@RequestParam(required = false) Long warehouseId,
                                                      @RequestParam(required = false) Long projectId,
                                                      @RequestParam(required = false) String actionType) {
        return Result.success(deviceInventoryService.logList(warehouseId, projectId, actionType));
    }
}
