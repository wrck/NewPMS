package com.vibe.device.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.DeviceInventoryActionDTO;
import com.vibe.device.dto.export.DeviceInventoryLogExportDTO;
import com.vibe.device.service.DeviceInventoryService;
import com.vibe.device.vo.DeviceInventoryLogVO;
import com.vibe.utils.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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

    @Operation(summary = "导出出入库流水（Excel）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_INVENTORY, type = "EXPORT", description = "导出出入库流水")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/logs/export")
    public void exportLogs(HttpServletResponse response,
                           @RequestParam(required = false) Long warehouseId,
                           @RequestParam(required = false) Long projectId,
                           @RequestParam(required = false) String actionType) throws IOException {
        List<DeviceInventoryLogVO> records = deviceInventoryService.logList(warehouseId, projectId, actionType);
        List<DeviceInventoryLogExportDTO> data = records.stream().map(vo -> {
            DeviceInventoryLogExportDTO dto = new DeviceInventoryLogExportDTO();
            BeanUtils.copyProperties(vo, dto);
            return dto;
        }).collect(Collectors.toList());
        ExcelUtils.export(response, "设备出入库流水", "出入库流水", DeviceInventoryLogExportDTO.class, data);
    }
}
