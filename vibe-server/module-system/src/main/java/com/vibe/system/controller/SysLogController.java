package com.vibe.system.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.system.dto.SysLogQueryDTO;
import com.vibe.system.service.SysLogService;
import com.vibe.system.vo.SysLogVO;
import com.vibe.system.vo.SysLoginLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志管理 Controller（操作日志 + 登录日志）
 *
 * @author vibe
 */
@Tag(name = "日志管理", description = "操作日志、登录日志查询")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;

    @Operation(summary = "分页查询操作日志")
    @PreAuthorize("@ss.hasPermi('system:log') or hasRole('SUPER_ADMIN')")
    @GetMapping("/logs")
    public Result<PageResult<SysLogVO>> pageOperationLog(@ParameterObject SysLogQueryDTO query) {
        return Result.success(sysLogService.pageOperationLog(query));
    }

    @Operation(summary = "分页查询登录日志")
    @PreAuthorize("@ss.hasPermi('system:log') or hasRole('SUPER_ADMIN')")
    @GetMapping("/logs/login")
    public Result<PageResult<SysLoginLogVO>> pageLoginLog(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startBegin,
            @RequestParam(required = false) String startEnd) {
        // 兼容前端字段名：userName/username、startBegin/beginTime、startEnd/endTime
        return Result.success(sysLogService.pageLoginLog(page, size, userName, status, startBegin, startEnd));
    }

    @Operation(summary = "分页查询登录日志（兼容旧版路径）")
    @PreAuthorize("@ss.hasPermi('system:log') or hasRole('SUPER_ADMIN')")
    @GetMapping("/login-logs")
    public Result<PageResult<SysLoginLogVO>> pageLoginLogLegacy(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(sysLogService.pageLoginLog(page, size, username, status, beginTime, endTime));
    }
}
