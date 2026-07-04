package com.vibe.finance.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.finance.dto.FinanceCostQueryDTO;
import com.vibe.finance.dto.FinanceCostSaveDTO;
import com.vibe.finance.service.FinanceCostService;
import com.vibe.finance.vo.FinanceCostVO;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 成本归集 Controller
 *
 * <p>路径：{@code /api/v1/finance/costs}</p>
 *
 * @author vibe
 */
@Tag(name = "成本归集", description = "人工/差旅/代理商/其他费用归集到项目维度")
@RestController
@RequestMapping("/api/v1/finance/costs")
@RequiredArgsConstructor
public class FinanceCostController {

    private final FinanceCostService financeCostService;

    @Operation(summary = "分页查询成本")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<FinanceCostVO>> page(@ParameterObject FinanceCostQueryDTO query) {
        return Result.success(financeCostService.page(query));
    }

    @Operation(summary = "成本详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<FinanceCostVO> detail(@PathVariable Long id) {
        return Result.success(financeCostService.getDetail(id));
    }

    @Operation(summary = "创建成本")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody FinanceCostSaveDTO dto) {
        return Result.success(financeCostService.save(dto));
    }

    @Operation(summary = "更新成本")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FinanceCostSaveDTO dto) {
        financeCostService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除成本")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','FINANCE')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        financeCostService.delete(id);
        return Result.success();
    }
}
