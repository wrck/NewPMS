package com.vibe.report.controller;

import com.vibe.common.result.Result;
import com.vibe.report.dto.DeviceReportQueryDTO;
import com.vibe.report.dto.FinanceReportQueryDTO;
import com.vibe.report.dto.ProjectReportQueryDTO;
import com.vibe.report.dto.ResourceReportQueryDTO;
import com.vibe.report.service.BusinessReportService;
import com.vibe.report.vo.DeviceReportVO;
import com.vibe.report.vo.FinanceReportVO;
import com.vibe.report.vo.ProjectReportVO;
import com.vibe.report.vo.ResourceReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务报表 Controller
 *
 * <p>路径：{@code /api/v1/report}</p>
 * <p>提供项目、设备、资源、财务四类业务报表聚合数据。</p>
 *
 * @author vibe
 */
@Tag(name = "业务报表", description = "项目/设备/资源/财务四类业务报表")
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class BusinessReportController {

    private final BusinessReportService businessReportService;

    @Operation(summary = "项目报表", description = "项目维度统计：状态/产品线/区域分布、PM 业绩、明细列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @GetMapping("/project")
    public Result<ProjectReportVO> projectReport(@ParameterObject ProjectReportQueryDTO query) {
        return Result.success(businessReportService.getProjectReport(query));
    }

    @Operation(summary = "设备报表", description = "设备状态分布、产品线分布、BOM 完成率、库存状态")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @GetMapping("/device")
    public Result<DeviceReportVO> deviceReport(@ParameterObject DeviceReportQueryDTO query) {
        return Result.success(businessReportService.getDeviceReport(query));
    }

    @Operation(summary = "资源报表", description = "工程师负荷、工时统计、项目维度投入")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @GetMapping("/resource")
    public Result<ResourceReportVO> resourceReport(@ParameterObject ResourceReportQueryDTO query) {
        return Result.success(businessReportService.getResourceReport(query));
    }

    @Operation(summary = "财务报表", description = "收入/成本/利润汇总、按客户/区域/产品线分布、代理商结算")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','FINANCE')")
    @GetMapping("/finance")
    public Result<FinanceReportVO> financeReport(@ParameterObject FinanceReportQueryDTO query) {
        return Result.success(businessReportService.getFinanceReport(query));
    }
}
