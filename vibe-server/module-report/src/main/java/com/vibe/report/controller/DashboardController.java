package com.vibe.report.controller;

import com.vibe.common.result.Result;
import com.vibe.report.service.DashboardService;
import com.vibe.report.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台首页 Controller
 *
 * <p>根据当前登录用户角色返回差异化的首页数据。</p>
 *
 * @author vibe
 */
@Tag(name = "工作台首页", description = "按角色返回差异化首页数据")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "工作台首页", description = "根据当前用户角色返回总监/PM/工程师/代理商首页数据")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<DashboardVO> dashboard() {
        return Result.success(dashboardService.getDashboard());
    }
}
