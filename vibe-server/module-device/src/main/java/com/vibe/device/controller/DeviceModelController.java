package com.vibe.device.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.DeviceModelDTO;
import com.vibe.device.dto.DeviceModelQueryDTO;
import com.vibe.device.service.DeviceModelService;
import com.vibe.device.vo.DeviceModelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
 * 设备型号库 Controller
 *
 * <p>设备型号的维护与查询。DEVICE_ADMIN/SUPER_ADMIN 可增删改，
 * PM/ENGINEER 只读查询（用于设备录入/导入时选择型号）。</p>
 *
 * @author vibe
 */
@Tag(name = "设备型号库", description = "型号维护、产品线/类别筛选")
@RestController
@RequestMapping("/api/v1/devices/models")
@RequiredArgsConstructor
public class DeviceModelController {

    private final DeviceModelService deviceModelService;

    @Operation(summary = "分页查询型号")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping
    public Result<PageResult<DeviceModelVO>> page(@ParameterObject DeviceModelQueryDTO query) {
        return Result.success(deviceModelService.page(query));
    }

    @Operation(summary = "查询全部型号（下拉选项）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/all")
    public Result<List<DeviceModelVO>> listAll() {
        return Result.success(deviceModelService.listAll());
    }

    @Operation(summary = "按产品线/类别查询型号")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/search")
    public Result<List<DeviceModelVO>> list(@RequestParam(required = false) String productLine,
                                            @RequestParam(required = false) String category) {
        return Result.success(deviceModelService.list(productLine, category));
    }

    @Operation(summary = "型号详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/{id}")
    public Result<DeviceModelVO> detail(@PathVariable Long id) {
        return Result.success(deviceModelService.getDetail(id));
    }

    @Operation(summary = "新增型号")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_MODEL, type = "CREATE", description = "新增设备型号")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody DeviceModelDTO dto) {
        return Result.success(deviceModelService.create(dto));
    }

    @Operation(summary = "编辑型号")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_MODEL, type = "UPDATE", description = "编辑设备型号")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeviceModelDTO dto) {
        dto.setId(id);
        deviceModelService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除型号")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_MODEL, type = "DELETE", description = "删除设备型号")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceModelService.delete(id);
        return Result.success();
    }
}
