package com.vibe.device.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.device.constant.DeviceConstant;
import com.vibe.device.dto.DeviceBomDTO;
import com.vibe.device.service.DeviceBomService;
import com.vibe.device.vo.DeviceBomVO;
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
 * 项目设备清单（BOM）Controller
 *
 * <p>BOM 维护、进度统计、型号变更。DEVICE_ADMIN/SUPER_ADMIN 可增删改，
 * PM/ENGINEER 只读查询。</p>
 *
 * @author vibe
 */
@Tag(name = "项目设备清单", description = "BOM 维护、进度统计、型号变更")
@RestController
@RequestMapping("/api/v1/devices/boms")
@RequiredArgsConstructor
public class DeviceBomController {

    private final DeviceBomService deviceBomService;

    @Operation(summary = "查询项目 BOM 列表（含进度数量）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping
    public Result<List<DeviceBomVO>> listByProject(@RequestParam(required = false) Long projectId) {
        // projectId 为空时返回空列表，避免无项目维度的全表扫描
        if (projectId == null) {
            return Result.success(java.util.Collections.emptyList());
        }
        return Result.success(deviceBomService.listByProject(projectId));
    }

    @Operation(summary = "BOM 详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/{id}")
    public Result<DeviceBomVO> detail(@PathVariable Long id) {
        return Result.success(deviceBomService.getDetail(id));
    }

    @Operation(summary = "BOM 进度统计（按型号维度的到货/安装/验收数量）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping("/progress")
    public Result<List<DeviceBomVO>> statProgress(@RequestParam Long projectId) {
        return Result.success(deviceBomService.statProgress(projectId));
    }

    @Operation(summary = "新增/维护 BOM 行（同项目同型号唯一，存在则累加计划数量）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_BOM, type = "CREATE", description = "新增 BOM 行")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody DeviceBomDTO dto) {
        return Result.success(deviceBomService.save(dto));
    }

    @Operation(summary = "编辑 BOM 行（变更计划数量/备注）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_BOM, type = "UPDATE", description = "编辑 BOM 行")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeviceBomDTO dto) {
        dto.setId(id);
        deviceBomService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除 BOM 行")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_BOM, type = "DELETE", description = "删除 BOM 行")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceBomService.delete(id);
        return Result.success();
    }

    @Operation(summary = "BOM 变更（增加/减少/替换型号）")
    @OperationLog(module = DeviceConstant.MODULE_DEVICE_BOM, type = "UPDATE", description = "BOM 型号变更")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN')")
    @PutMapping("/change")
    public Result<Void> changeBom(@RequestParam Long projectId,
                                  @RequestParam(required = false) Long fromModelId,
                                  @RequestParam(required = false) Long toModelId,
                                  @RequestParam int deltaQty,
                                  @RequestParam(required = false) String remark) {
        deviceBomService.changeBom(projectId, fromModelId, toModelId, deltaQty, remark);
        return Result.success();
    }
}
