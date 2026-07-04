package com.vibe.delivery.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.delivery.dto.CutoverApprovalDTO;
import com.vibe.delivery.dto.CutoverCompleteDTO;
import com.vibe.delivery.dto.CutoverPlanCreateDTO;
import com.vibe.delivery.dto.CutoverPlanQueryDTO;
import com.vibe.delivery.dto.CutoverStepExecuteDTO;
import com.vibe.delivery.service.CutoverPlanService;
import com.vibe.delivery.vo.CutoverExecutionLogVO;
import com.vibe.delivery.vo.CutoverPlanDetailVO;
import com.vibe.delivery.vo.CutoverPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

import java.util.List;

/**
 * 割接方案 Controller（割接管理全流程：编制→内部审批→客户审批→执行→完成/中止）
 *
 * <p>路径：{@code /api/v1/cutover/plans}</p>
 *
 * <p>权限：</p>
 * <ul>
 *   <li>编制/更新/删除/提交审批：PM / 调度员 / 管理员</li>
 *   <li>内部审批：技术主管/总监（DIRECTOR）/ 管理员</li>
 *   <li>执行步骤/完成/中止：PM / DIRECTOR / 管理员</li>
 *   <li>客户审批：CUSTOMER 角色（由客户门户调用，见 CustomerPortalController）</li>
 * </ul>
 *
 * @author vibe
 */
@Tag(name = "割接管理", description = "割接方案编制/审批/执行/总结全流程")
@RestController
@RequestMapping("/api/v1/cutover/plans")
@RequiredArgsConstructor
public class CutoverPlanController {

    private final CutoverPlanService cutoverPlanService;

    /* ============ 基础 CRUD ============ */

    @Operation(summary = "分页查询割接方案")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<CutoverPlanVO>> page(@ParameterObject CutoverPlanQueryDTO query) {
        return Result.success(cutoverPlanService.page(query));
    }

    @Operation(summary = "割接方案详情（含步骤/操作日志）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<CutoverPlanDetailVO> detail(
            @Parameter(name = "id", description = "割接方案ID", required = true, in = ParameterIn.PATH)
            @PathVariable Long id) {
        return Result.success(cutoverPlanService.getDetail(id));
    }

    @Operation(summary = "创建割接方案（含步骤）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CutoverPlanCreateDTO dto) {
        return Result.success(cutoverPlanService.create(dto));
    }

    @Operation(summary = "更新割接方案（仅草稿可改）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody CutoverPlanCreateDTO dto) {
        cutoverPlanService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除割接方案（仅草稿可删）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cutoverPlanService.delete(id);
        return Result.success();
    }

    /* ============ 审批流程 ============ */

    @Operation(summary = "提交内部审批（DRAFT → PENDING_INTERNAL_APPROVAL）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/submit-internal-approval")
    public Result<Void> submitInternalApproval(@PathVariable Long id) {
        cutoverPlanService.submitInternalApproval(id);
        return Result.success();
    }

    @Operation(summary = "内部审批通过（技术主管/总监）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/internal-approve")
    public Result<Void> internalApprove(@Valid @RequestBody CutoverApprovalDTO dto) {
        cutoverPlanService.internalApprove(dto);
        return Result.success();
    }

    @Operation(summary = "内部审批驳回（技术主管/总监）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/internal-reject")
    public Result<Void> internalReject(@Valid @RequestBody CutoverApprovalDTO dto) {
        cutoverPlanService.internalReject(dto);
        return Result.success();
    }

    @Operation(summary = "发起客户审批（生成客户签核链接token）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/start-customer-approval")
    public Result<String> startCustomerApproval(@PathVariable Long id) {
        return Result.success(cutoverPlanService.startCustomerApproval(id));
    }

    /* ============ 执行流程 ============ */

    @Operation(summary = "开始执行割接（CUSTOMER_APPROVED → EXECUTING）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/start-execution")
    public Result<Void> startExecution(@PathVariable Long id) {
        cutoverPlanService.startExecution(id);
        return Result.success();
    }

    @Operation(summary = "执行步骤（PENDING→EXECUTING 或 EXECUTING→COMPLETED，幂等二次调用即完成）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','ENGINEER')")
    @PostMapping("/execute-step")
    public Result<Void> executeStep(@Valid @RequestBody CutoverStepExecuteDTO dto) {
        cutoverPlanService.executeStep(dto);
        return Result.success();
    }

    @Operation(summary = "回退步骤（执行回退方案，EXECUTING → ROLLED_BACK）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/rollback-step")
    public Result<Void> rollbackStep(@Valid @RequestBody CutoverStepExecuteDTO dto) {
        cutoverPlanService.rollbackStep(dto);
        return Result.success();
    }

    @Operation(summary = "步骤异常（EXECUTING → ABORTED，记录异常信息）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','ENGINEER')")
    @PostMapping("/exception-step")
    public Result<Void> exceptionStep(@Valid @RequestBody CutoverStepExecuteDTO dto) {
        cutoverPlanService.exceptionStep(dto);
        return Result.success();
    }

    @Operation(summary = "完成割接（所有步骤完成后提交总结）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/complete")
    public Result<Void> complete(@Valid @RequestBody CutoverCompleteDTO dto) {
        cutoverPlanService.complete(dto);
        return Result.success();
    }

    @Operation(summary = "中止割接（紧急情况，任意状态可中止）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/abort")
    public Result<Void> abort(@PathVariable Long id,
                              @RequestParam(required = false) String remark) {
        cutoverPlanService.abort(id, remark);
        return Result.success();
    }

    /* ============ 查询 ============ */

    @Operation(summary = "查询割接方案操作日志")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/logs")
    public Result<List<CutoverExecutionLogVO>> listLogs(@PathVariable Long id) {
        return Result.success(cutoverPlanService.listLogs(id));
    }
}
