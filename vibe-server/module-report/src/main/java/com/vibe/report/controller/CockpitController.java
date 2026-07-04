package com.vibe.report.controller;

import com.vibe.common.result.Result;
import com.vibe.report.service.ManagementCockpitService;
import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.CockpitAggregatedVO;
import com.vibe.report.vo.CockpitStatVO;
import com.vibe.report.vo.RiskProjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理驾驶舱 Controller
 *
 * <p>核心指标卡片、项目阶段分布、项目趋势、风险项目。
 * 提供聚合端点 {@code GET /cockpit} 一次性返回全部数据，
 * 也保留 4 个细分端点供按需加载。</p>
 *
 * @author vibe
 */
@Tag(name = "管理驾驶舱", description = "核心指标、项目阶段分布、项目趋势、风险项目")
@RestController
@RequestMapping("/api/v1/cockpit")
@RequiredArgsConstructor
public class CockpitController {

    private final ManagementCockpitService managementCockpitService;

    @Operation(summary = "驾驶舱聚合数据", description = "一次性返回 KPI、阶段分布、项目趋势、风险项目、待办、动态")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<CockpitAggregatedVO> aggregated() {
        return Result.success(managementCockpitService.getAggregated());
    }

    @Operation(summary = "核心指标卡片", description = "项目/设备/工程师/代理商数，含环比增长率")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stats")
    public Result<CockpitStatVO> stats() {
        return Result.success(managementCockpitService.getStats());
    }

    @Operation(summary = "项目阶段分布", description = "按当前阶段统计项目数（饼图）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/project-phases")
    public Result<List<ChartDataVO>> projectPhases() {
        return Result.success(managementCockpitService.getProjectPhaseDistribution());
    }

    @Operation(summary = "项目趋势", description = "近12月项目新增/完成趋势（折线图）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/project-trend")
    public Result<List<ChartDataVO>> projectTrend() {
        return Result.success(managementCockpitService.getProjectTrend());
    }

    @Operation(summary = "风险项目列表", description = "进度滞后/超期任务/未解决问题/项目超期")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/risk-projects")
    public Result<List<RiskProjectVO>> riskProjects() {
        return Result.success(managementCockpitService.getRiskProjects());
    }
}
