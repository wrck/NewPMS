package com.vibe.agent.controller;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.OutsourceTaskActionDTO;
import com.vibe.agent.dto.OutsourceTaskCreateDTO;
import com.vibe.agent.dto.OutsourceTaskQueryDTO;
import com.vibe.agent.service.OutsourceTaskService;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.annotation.OperationLog;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.service.FlowableProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * <p><b>Flowable 集成（增量增强）：</b>
 * <ul>
 *   <li>{@link #create}：PM 创建转包任务后启动 Flowable {@code outsource} 流程</li>
 *   <li>{@link #accept} / {@link #reject}：代理商接单/拒绝，完成 Flowable 的"代理商接单"任务</li>
 *   <li>{@link #confirm} / {@link #returnTask}：PM 审核，完成 Flowable 的"交付物审核"任务</li>
 * </ul>
 * Flowable 操作采用 try/catch 兜底，若引擎异常不影响原状态机流转。</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "转包任务管理", description = "创建/列表/详情/接单/拒绝/指派/退回/重新提交")
@RestController
@RequestMapping("/api/v1/outsource-tasks")
@RequiredArgsConstructor
public class OutsourceTaskController {

    /** Flowable 流程定义 key：转包任务流程 */
    private static final String PROCESS_KEY = "outsource";

    private final OutsourceTaskService outsourceTaskService;
    private final FlowableProcessService flowableProcessService;

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
        Long taskId = outsourceTaskService.create(dto);
        // Flowable 增强：启动转包流程（异常不阻断主流程）
        startFlowableSafely(taskId);
        return Result.success(taskId);
    }

    @Operation(summary = "代理商接单（PENDING → ACCEPTED）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "代理商接单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN')")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        outsourceTaskService.accept(id);
        // Flowable 增强：完成"代理商接单"任务（accepted=true）
        completeAcceptTaskSafely(id, true);
        return Result.success();
    }

    @Operation(summary = "代理商拒绝接单（PENDING → REJECTED）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "代理商拒绝接单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN')")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody(required = false) OutsourceTaskActionDTO dto) {
        outsourceTaskService.reject(id, dto != null ? dto : new OutsourceTaskActionDTO());
        // Flowable 增强：完成"代理商接单"任务（accepted=false）
        completeAcceptTaskSafely(id, false);
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
        // Flowable 增强：完成"交付物审核"任务（approved=true → 流程结束）
        completeReviewTaskSafely(id, true, null);
        return Result.success();
    }

    @Operation(summary = "PM 审核退回（SUBMITTED → RETURNED）")
    @OperationLog(module = AgentConstant.MODULE_OUTSOURCE_TASK, type = "UPDATE", description = "PM 审核退回")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{id}/return")
    public Result<Void> returnTask(@PathVariable Long id,
                                   @RequestBody OutsourceTaskActionDTO dto) {
        outsourceTaskService.returnTask(id, dto);
        // Flowable 增强：完成"交付物审核"任务（approved=false → 回到执行节点）
        completeReviewTaskSafely(id, false, dto != null ? dto.getReason() : null);
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

    /* ============ Flowable 集成辅助方法（兜底，异常仅记录日志） ============ */

    private void startFlowableSafely(Long taskId) {
        if (taskId == null) {
            return;
        }
        try {
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                log.warn("[Flowable] 启动转包流程失败：当前用户上下文为空，taskId={}", taskId);
                return;
            }
            Map<String, Object> variables = new HashMap<>();
            variables.put(FlowableProcessService.VAR_INITIATOR, String.valueOf(userId));
            flowableProcessService.startProcess(PROCESS_KEY, String.valueOf(taskId), variables);
            log.info("[Flowable] 转包流程已启动：taskId={}, initiator={}", taskId, userId);
        } catch (Exception e) {
            log.error("[Flowable] 启动转包流程异常（不影响主流程）：taskId={}", taskId, e);
        }
    }

    /**
     * 完成 Flowable 的"代理商接单"任务（设置 accepted 变量）。
     */
    private void completeAcceptTaskSafely(Long taskId, boolean accepted) {
        if (taskId == null) {
            return;
        }
        try {
            List<Task> activeTasks = flowableProcessService.findActiveTasksByBusinessKey(
                    PROCESS_KEY, String.valueOf(taskId));
            if (activeTasks.isEmpty()) {
                log.warn("[Flowable] 未找到活动任务：taskId={}, processKey={}", taskId, PROCESS_KEY);
                return;
            }
            Task task = activeTasks.get(0);
            Map<String, Object> variables = new HashMap<>();
            variables.put("accepted", accepted);
            flowableProcessService.approve(task.getId(), variables);
            log.info("[Flowable] 转包流程接单任务完成：taskId={}, flowableTaskId={}, accepted={}",
                    taskId, task.getId(), accepted);
        } catch (Exception e) {
            log.error("[Flowable] 完成转包接单任务异常（不影响主流程）：taskId={}", taskId, e);
        }
    }

    /**
     * 完成 Flowable 的"交付物审核"任务（设置 approved 变量）。
     */
    private void completeReviewTaskSafely(Long taskId, boolean approved, String remark) {
        if (taskId == null) {
            return;
        }
        try {
            List<Task> activeTasks = flowableProcessService.findActiveTasksByBusinessKey(
                    PROCESS_KEY, String.valueOf(taskId));
            if (activeTasks.isEmpty()) {
                log.warn("[Flowable] 未找到活动任务：taskId={}, processKey={}", taskId, PROCESS_KEY);
                return;
            }
            Task task = activeTasks.get(0);
            Map<String, Object> variables = new HashMap<>();
            if (remark != null) {
                variables.put("remark", remark);
            }
            if (approved) {
                flowableProcessService.approve(task.getId(), variables);
            } else {
                flowableProcessService.reject(task.getId(), remark);
            }
            log.info("[Flowable] 转包流程审核任务完成：taskId={}, flowableTaskId={}, approved={}",
                    taskId, task.getId(), approved);
        } catch (Exception e) {
            log.error("[Flowable] 完成转包审核任务异常（不影响主流程）：taskId={}", taskId, e);
        }
    }
}
