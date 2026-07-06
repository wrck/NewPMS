package com.vibe.finance.controller;

import com.vibe.common.result.Result;
import com.vibe.finance.dto.export.FinanceProfitExportDTO;
import com.vibe.finance.service.FinanceProfitService;
import com.vibe.finance.vo.FinanceProfitVO;
import com.vibe.utils.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 利润分析 Controller
 *
 * <p>路径：{@code /api/v1/finance/profits}</p>
 *
 * @author vibe
 */
@Tag(name = "利润分析", description = "项目利润/毛利率/维度分析/趋势分析")
@RestController
@RequestMapping("/api/v1/finance/profits")
@RequiredArgsConstructor
public class FinanceProfitController {

    private final FinanceProfitService financeProfitService;

    @Operation(summary = "查询指定项目的利润分析")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/projects/{projectId}")
    public Result<FinanceProfitVO> getProjectProfit(@PathVariable Long projectId) {
        return Result.success(financeProfitService.getProjectProfit(projectId));
    }

    @Operation(summary = "查询全部项目利润分析")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/projects")
    public Result<List<FinanceProfitVO>> listProjectProfit() {
        return Result.success(financeProfitService.listProjectProfit());
    }

    @Operation(summary = "按客户维度统计利润")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-customer")
    public Result<List<FinanceProfitVO>> listByCustomer() {
        return Result.success(financeProfitService.listProfitByCustomer());
    }

    @Operation(summary = "按区域维度统计利润")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-region")
    public Result<List<FinanceProfitVO>> listByRegion() {
        return Result.success(financeProfitService.listProfitByRegion());
    }

    @Operation(summary = "按产品线维度统计利润")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-product-line")
    public Result<List<FinanceProfitVO>> listByProductLine() {
        return Result.success(financeProfitService.listProfitByProductLine());
    }

    @Operation(summary = "导出项目利润分析列表（Excel）")
    @com.vibe.annotation.OperationLog(module = "利润分析", type = "EXPORT", description = "导出项目利润分析列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        List<FinanceProfitVO> records = financeProfitService.listProjectProfit();
        List<FinanceProfitExportDTO> data = records.stream().map(vo -> {
            FinanceProfitExportDTO dto = new FinanceProfitExportDTO();
            BeanUtils.copyProperties(vo, dto);
            return dto;
        }).collect(Collectors.toList());
        ExcelUtils.export(response, "项目利润分析", "利润分析", FinanceProfitExportDTO.class, data);
    }
}
