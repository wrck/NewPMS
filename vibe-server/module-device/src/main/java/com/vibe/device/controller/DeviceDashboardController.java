package com.vibe.device.controller;

import com.vibe.common.result.Result;
import com.vibe.device.service.DeviceDashboardService;
import com.vibe.device.vo.DeviceDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备看板 Controller
 *
 * <p>设备状态看板：项目维度完成率、状态分布、异常设备列表、多维统计。
 * 支持全局看板（projectId 为空）与项目看板（projectId 非空）。</p>
 *
 * @author vibe
 */
@Tag(name = "设备看板", description = "状态分布、BOM 完成率、异常设备、型号统计")
@RestController
@RequestMapping("/api/v1/devices/dashboard")
@RequiredArgsConstructor
public class DeviceDashboardController {

    private final DeviceDashboardService deviceDashboardService;

    @Operation(summary = "设备看板（项目维度完成率、状态分布、异常设备列表、多维统计）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEVICE_ADMIN','PM','ENGINEER')")
    @GetMapping
    public Result<DeviceDashboardVO> dashboard(@RequestParam(required = false) Long projectId) {
        return Result.success(deviceDashboardService.getDashboard(projectId));
    }
}
