package com.vibe.agent.controller;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.OutsourceTaskActionDTO;
import com.vibe.agent.dto.OutsourceTaskCreateDTO;
import com.vibe.agent.dto.OutsourceTaskQueryDTO;
import com.vibe.agent.service.OutsourceTaskService;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 转包任务管理 Controller
 *
 * <p>路径：{@code /api/v1/outsource-tasks}</p>
 *
 * <p><b>权限分工：</b></p>
 * <ul>
 *   <li>PM / SUPER_ADMIN：创建转包任务、审核交付物、打分</li>
 *   <li>AGENT_ADMIN：接单/拒绝、指派工程师、提交交付物</li>
 *   <li>AGENT_ENGINEER：查看分配给自己的任务</li>
 * </ul>
 *
 * <p><b>状态机：</b>PENDING → ACCEPTED → IN_PROGRESS → SUBMITTED → CONFIRMED，
 * 异常分支 REJECTED / RETURNED / OVERDUE。非法流转返回 40902 错误。</p>
 *
 * @author vibe
 */
@Tag(name = "转包任务管理", description = "创建/列表/详情/接单/拒绝/指派/退回/重新提交")
@RestController
@RequestMapping("/api/v1/outsource-tasks")
@RequiredArgsConstructor
public class OutsourceTaskController {

    private final OutsourceTaskService outsourceTaskService;

    @Operation(summary = "分页查询转包任务（数据权限：代理商仅看本公司/自己的任务）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping
    public Result<PageResult<OutsourceTaskVO>> page(@ParameterObject OutsourceTaskQueryDTO query) {
        return Result.success(outsourceTaskService.page(query));
    }

    @Operation(summary = "转包任务详情（对代理商角色脱敏客户/合同/成本字段）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping("/{id}")
    public Result<OutsourceTaskVO> detail(@PathVariable Long id) {
        return Result.success(outsourceTaskService.getDetail(id));
    }

    @Operation(summary = "创建转包任务（PM 指定代理商、任务范围、截止日期）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "CREATE", description = "创建转包任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OutsourceTaskCreateDTO dto) {
        return Result.success(outsourceTaskService.create(dto));
    }

    @Operation(summary = "代理商接单（PENDING → ACCEPTED）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "代理商接单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN')")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        outsourceTaskService.accept(id);
        return Result.success();
    }

    @Operation(summary = "代理商拒绝接单（PENDING → REJECTED）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "代理商拒绝接单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN')")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody(required = false) OutsourceTaskActionDTO dto) {
        outsourceTaskService.reject(id, dto != null ? dto : new OutsourceTaskActionDTO());
        return Result.success();
    }

    @Operation(summary = "代理商指派工程师（ACCEPTED → IN_PROGRESS）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "代理商指派工程师")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN')")
    @PutMapping("/{id}/assign")
    public Result<Void> assignEngineer(@PathVariable Long id,
                                       @Valid @RequestBody OutsourceTaskActionDTO dto) {
        outsourceTaskService.assignEngineer(id, dto);
        return Result.success();
    }

    @Operation(summary = "PM 审核通过（SUBMITTED → CONFIRMED）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "PM 审核通过")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        outsourceTaskService.confirm(id);
        return Result.success();
    }

    @Operation(summary = "PM 审核退回（SUBMITTED → RETURNED）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "PM 审核退回")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{id}/return")
    public Result<Void> returnTask(@PathVariable Long id,
                                   @RequestBody OutsourceTaskActionDTO dto) {
        outsourceTaskService.returnTask(id, dto);
        return Result.success();
    }

    @Operation(summary = "代理商重新提交（RETURNED → IN_PROGRESS）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "代理商重新提交")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN')")
    @PutMapping("/{id}/resubmit")
    public Result<Void> resubmit(@PathVariable Long id) {
        outsourceTaskService.resubmit(id);
        return Result.success();
    }

    @Operation(summary = "按代理商公司查询任务列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN')")
    @GetMapping("/by-company/{companyId}")
    public Result<java.util.List<OutsourceTaskVO>> listByCompany(@PathVariable Long companyId) {
        return Result.success(outsourceTaskService.listByAgentCompany(companyId));
    }

    @Operation(summary = "定时任务：扫描超期任务并标记为 OVERDUE（仅供定时任务/管理员调用）")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/mark-overdue")
    public Result<Integer> markOverdue() {
        return Result.success(outsourceTaskService.markOverdueTasks());
    }
}
