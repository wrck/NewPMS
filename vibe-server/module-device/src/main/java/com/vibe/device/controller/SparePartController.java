package com.vibe.device.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.SparePartActionDTO;
import com.vibe.device.dto.SparePartDTO;
import com.vibe.device.service.SparePartService;
import com.vibe.device.vo.SparePartLogVO;
import com.vibe.device.vo.SparePartVO;
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
 * 备件管理 Controller
 *
 * <p>备件台账维护与操作（入库/领用/归还/返修），同步更新库存数量并记录流水。
 * DEVICE_ADMIN/SUPER_ADMIN 可增删改与操作，PM/ENGINEER 只读查询。</p>
 *
 * @author vibe
 */
@Tag(name = "备件管理", description = "备件台账、入库/领用/归还/返修、流水查询")
@RestController
@RequestMapping("/api/v1/devices/spare-parts")
@RequiredArgsConstructor
public class SparePartController {

    private final SparePartService sparePartService;

    @Operation(summary = "分页查询备件台账")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping
    public Result<PageResult<SparePartVO>> page(@RequestParam(required = false, defaultValue = "1") Integer page,
                                                @RequestParam(required = false, defaultValue = "20") Integer size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long warehouseId,
                                                @RequestParam(required = false) Long modelId) {
        return Result.success(sparePartService.page(page, size, keyword, warehouseId, modelId));
    }

    @Operation(summary = "备件详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/{id}")
    public Result<SparePartVO> detail(@PathVariable Long id) {
        return Result.success(sparePartService.getDetail(id));
    }

    @Operation(summary = "新增备件")
    @OperationLog(module = DeviceConstant.MODULE_SPARE_PART, type = "CREATE", description = "新增备件")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SparePartDTO dto) {
        return Result.success(sparePartService.create(dto));
    }

    @Operation(summary = "编辑备件")
    @OperationLog(module = DeviceConstant.MODULE_SPARE_PART, type = "UPDATE", description = "编辑备件")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SparePartDTO dto) {
        dto.setId(id);
        sparePartService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除备件")
    @OperationLog(module = DeviceConstant.MODULE_SPARE_PART, type = "DELETE", description = "删除备件")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sparePartService.delete(id);
        return Result.success();
    }

    @Operation(summary = "备件操作（入库/领用/归还/返修，同步更新库存并记录流水）")
    @OperationLog(module = DeviceConstant.MODULE_SPARE_PART, type = "UPDATE", description = "备件操作")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PostMapping("/actions")
    public Result<Void> action(@Valid @RequestBody SparePartActionDTO dto) {
        sparePartService.action(dto);
        return Result.success();
    }

    @Operation(summary = "备件操作流水查询")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/logs")
    public Result<List<SparePartLogVO>> logList(@RequestParam(required = false) Long sparePartId,
                                                @RequestParam(required = false) Long projectId,
                                                @RequestParam(required = false) String actionType) {
        return Result.success(sparePartService.logList(sparePartId, projectId, actionType));
    }
}
