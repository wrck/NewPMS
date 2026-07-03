package com.vibe.device.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.WarehouseDTO;
import com.vibe.device.service.WarehouseService;
import com.vibe.device.vo.WarehouseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仓库管理 Controller
 *
 * <p>仓库的维护与查询，含安全库存配置（safety_stock JSON）。
 * DEVICE_ADMIN/SUPER_ADMIN 可增删改，PM/ENGINEER 只读。</p>
 *
 * @author vibe
 */
@Tag(name = "仓库管理", description = "仓库维护、安全库存配置")
@RestController
@RequestMapping("/api/v1/devices/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "分页查询仓库")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping
    public Result<PageResult<WarehouseVO>> page(@RequestParam(required = false, defaultValue = "1") Integer page,
                                                @RequestParam(required = false, defaultValue = "20") Integer size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String region) {
        return Result.success(warehouseService.page(page, size, keyword, region));
    }

    @Operation(summary = "查询全部仓库（下拉选项）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/all")
    public Result<List<WarehouseVO>> listAll() {
        return Result.success(warehouseService.listAll());
    }

    @Operation(summary = "仓库详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/{id}")
    public Result<WarehouseVO> detail(@PathVariable Long id) {
        return Result.success(warehouseService.getDetail(id));
    }

    @Operation(summary = "新增仓库")
    @OperationLog(module = DeviceConstant.MODULE_WAREHOUSE, type = "CREATE", description = "新增仓库")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody WarehouseDTO dto) {
        return Result.success(warehouseService.create(dto));
    }

    @Operation(summary = "编辑仓库（含安全库存配置）")
    @OperationLog(module = DeviceConstant.MODULE_WAREHOUSE, type = "UPDATE", description = "编辑仓库")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WarehouseDTO dto) {
        dto.setId(id);
        warehouseService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除仓库")
    @OperationLog(module = DeviceConstant.MODULE_WAREHOUSE, type = "DELETE", description = "删除仓库")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.success();
    }
}
