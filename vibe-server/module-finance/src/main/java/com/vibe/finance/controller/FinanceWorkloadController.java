package com.vibe.finance.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.finance.dto.FinanceWorkloadQueryDTO;
import com.vibe.finance.dto.FinanceWorkloadSaveDTO;
import com.vibe.finance.service.FinanceWorkloadService;
import com.vibe.finance.vo.FinanceWorkloadConfirmationVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代理商结算 Controller
 *
 * <p>路径：{@code /api/v1/finance/settlements}</p>
 *
 * @author vibe
 */
@Tag(name = "代理商结算", description = "工作量确认单/费用计算/对账/审批流/付款跟踪")
@RestController
@RequestMapping("/api/v1/finance/settlements")
@RequiredArgsConstructor
public class FinanceWorkloadController {

    private final FinanceWorkloadService financeWorkloadService;

    @Operation(summary = "分页查询结算单")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<FinanceWorkloadConfirmationVO>> page(@ParameterObject FinanceWorkloadQueryDTO query) {
        return Result.success(financeWorkloadService.page(query));
    }

    @Operation(summary = "结算单详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<FinanceWorkloadConfirmationVO> detail(@PathVariable Long id) {
        return Result.success(financeWorkloadService.getDetail(id));
    }

    @Operation(summary = "创建结算单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody FinanceWorkloadSaveDTO dto) {
        return Result.success(financeWorkloadService.save(dto));
    }

    @Operation(summary = "更新结算单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','FINANCE')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FinanceWorkloadSaveDTO dto) {
        financeWorkloadService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除结算单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','FINANCE')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        financeWorkloadService.delete(id);
        return Result.success();
    }

    @Operation(summary = "PM 确认工作量")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/pm-confirm")
    public Result<Void> pmConfirm(@PathVariable Long id) {
        financeWorkloadService.pmConfirm(id);
        return Result.success();
    }

    @Operation(summary = "代理商确认工作量")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN')")
    @PostMapping("/{id}/agent-confirm")
    public Result<Void> agentConfirm(@PathVariable Long id) {
        financeWorkloadService.agentConfirm(id);
        return Result.success();
    }

    @Operation(summary = "总监审批")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/{id}/director-approve")
    public Result<Void> directorApprove(@PathVariable Long id,
                                        @RequestParam boolean passed,
                                        @RequestParam(required = false) String remark) {
        financeWorkloadService.directorApprove(id, passed, remark);
        return Result.success();
    }

    @Operation(summary = "财务审批")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FINANCE')")
    @PostMapping("/{id}/finance-approve")
    public Result<Void> financeApprove(@PathVariable Long id,
                                       @RequestParam boolean passed,
                                       @RequestParam(required = false) String remark) {
        financeWorkloadService.financeApprove(id, passed, remark);
        return Result.success();
    }

    @Operation(summary = "更新付款状态")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FINANCE')")
    @PostMapping("/{id}/payment-status")
    public Result<Void> updatePaymentStatus(@PathVariable Long id,
                                             @RequestParam String paymentStatus) {
        financeWorkloadService.updatePaymentStatus(id, paymentStatus);
        return Result.success();
    }
}
