package com.vibe.finance.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.finance.dto.FinanceBudgetQueryDTO;
import com.vibe.finance.dto.FinanceBudgetSaveDTO;
import com.vibe.finance.dto.export.FinanceBudgetExportDTO;
import com.vibe.finance.service.FinanceBudgetService;
import com.vibe.finance.vo.FinanceBudgetVO;
import com.vibe.utils.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.BeanUtils;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目预算 Controller
 *
 * <p>路径：{@code /api/v1/finance/budgets}</p>
 *
 * @author vibe
 */
@Tag(name = "项目预算管理", description = "预算编制/审批/调整/对比")
@RestController
@RequestMapping("/api/v1/finance/budgets")
@RequiredArgsConstructor
public class FinanceBudgetController {

    private final FinanceBudgetService financeBudgetService;

    /** 单次导出最大行数 */
    private static final int EXPORT_MAX_ROWS = 10000;

    @Operation(summary = "分页查询预算")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<FinanceBudgetVO>> page(@ParameterObject FinanceBudgetQueryDTO query) {
        return Result.success(financeBudgetService.page(query));
    }

    @Operation(summary = "预算详情（含实际成本对比）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<FinanceBudgetVO> detail(@PathVariable Long id) {
        return Result.success(financeBudgetService.getDetail(id));
    }

    @Operation(summary = "创建预算")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody FinanceBudgetSaveDTO dto) {
        return Result.success(financeBudgetService.save(dto));
    }

    @Operation(summary = "更新预算")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FinanceBudgetSaveDTO dto) {
        financeBudgetService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除预算")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','FINANCE')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        financeBudgetService.delete(id);
        return Result.success();
    }

    @Operation(summary = "提交预算审批")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        financeBudgetService.submit(id);
        return Result.success();
    }

    @Operation(summary = "审批预算（通过/驳回）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam boolean passed,
                                @RequestParam(required = false) String remark) {
        financeBudgetService.approve(id, passed, remark);
        return Result.success();
    }

    @Operation(summary = "查询项目年度实际成本汇总")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/actual-cost")
    public Result<BigDecimal> actualCost(@RequestParam Long projectId,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) String costType) {
        return Result.success(financeBudgetService.sumActualCost(projectId, year, costType));
    }

    @Operation(summary = "导出预算列表（Excel）")
    @com.vibe.annotation.OperationLog(module = "项目预算", type = "EXPORT", description = "导出预算列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @GetMapping("/export")
    public void export(HttpServletResponse response, @ParameterObject FinanceBudgetQueryDTO query) throws IOException {
        query.setPage(1);
        query.setSize(EXPORT_MAX_ROWS);
        List<FinanceBudgetVO> records = financeBudgetService.page(query).getRecords();
        List<FinanceBudgetExportDTO> data = records.stream().map(vo -> {
            FinanceBudgetExportDTO dto = new FinanceBudgetExportDTO();
            BeanUtils.copyProperties(vo, dto);
            return dto;
        }).collect(Collectors.toList());
        ExcelUtils.export(response, "项目预算", "预算", FinanceBudgetExportDTO.class, data);
    }
}
