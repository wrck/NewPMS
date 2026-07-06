package com.vibe.acceptance.controller;

import com.vibe.acceptance.dto.AcceptanceTaskActionDTO;
import com.vibe.acceptance.dto.AcceptanceTaskCreateDTO;
import com.vibe.acceptance.dto.AcceptanceTaskQueryDTO;
import com.vibe.acceptance.service.AcceptanceTaskService;
import com.vibe.acceptance.vo.AcceptanceTaskVO;
import com.vibe.acceptance.vo.AcceptanceTestRecordVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 验收任务 Controller（验收流程：申请→内部审核→客户签核→完成）
 *
 * <p>路径：{@code /api/v1/acceptance/tasks}</p>
 *
 * <p><b>Flowable 集成（增量增强）：</b>关键审批端点在原有状态机逻辑基础上，
 * 同步驱动 Flowable {@code acceptance} 流程实例：
 * <ul>
 *   <li>{@link #apply}：启动 Flowable 流程，businessKey = 验收任务 ID</li>
 *   <li>{@link #internalAudit}：完成 Flowable 的"内部技术审核"任务</li>
 *   <li>{@link #customerSign}：完成 Flowable 的"客户签核"任务</li>
 * </ul>
 * Flowable 操作采用 try/catch 兜底，若引擎异常不影响原状态机流转。</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "验收任务管理", description = "验收申请/内部审核/客户签核/测试记录")
@RestController
@RequestMapping("/api/v1/acceptance/tasks")
@RequiredArgsConstructor
public class AcceptanceTaskController {

    /** Flowable 流程定义 key：验收审批流 */
    private static final String PROCESS_KEY = "acceptance";

    private final AcceptanceTaskService acceptanceTaskService;
    private final FlowableProcessService flowableProcessService;

    @Operation(summary = "分页查询验收任务")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<AcceptanceTaskVO>> page(@ParameterObject AcceptanceTaskQueryDTO query) {
        return Result.success(acceptanceTaskService.page(query));
    }

    @Operation(summary = "验收任务详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<AcceptanceTaskVO> detail(@PathVariable Long id) {
        return Result.success(acceptanceTaskService.getDetail(id));
    }

    @Operation(summary = "创建验收任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AcceptanceTaskCreateDTO dto) {
        return Result.success(acceptanceTaskService.create(dto));
    }

    @Operation(summary = "更新验收任务（仅草稿可改）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AcceptanceTaskCreateDTO dto) {
        acceptanceTaskService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除验收任务（仅草稿可删）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        acceptanceTaskService.delete(id);
        return Result.success();
    }

    @Operation(summary = "PM 提交验收申请")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody AcceptanceTaskActionDTO dto) {
        // 1. 原状态机流转（兜底主流程）
        acceptanceTaskService.apply(dto);
        // 2. Flowable 增强：启动验收审批流（异常不阻断主流程）
        startFlowableSafely(dto.getTaskId());
        return Result.success();
    }

    @Operation(summary = "内部技术审核")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/internal-audit")
    public Result<Void> internalAudit(@Valid @RequestBody AcceptanceTaskActionDTO dto) {
        // 1. 原状态机流转（兜底主流程）
        acceptanceTaskService.internalAudit(dto);
        // 2. Flowable 增强：完成"内部技术审核"任务
        completeFlowableTaskSafely(dto.getTaskId(), isApproved(dto.getResult()), dto.getRemark());
        return Result.success();
    }

    @Operation(summary = "发起客户签核")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/start-customer-sign")
    public Result<Void> startCustomerSign(@PathVariable Long id) {
        acceptanceTaskService.startCustomerSign(id);
        return Result.success();
    }

    @Operation(summary = "客户签核结果")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/customer-sign")
    public Result<Void> customerSign(@Valid @RequestBody AcceptanceTaskActionDTO dto) {
        // 1. 原状态机流转（兜底主流程）
        acceptanceTaskService.customerSign(dto);
        // 2. Flowable 增强：完成"客户签核"任务
        completeFlowableTaskSafely(dto.getTaskId(), isApproved(dto.getResult()), dto.getRemark());
        return Result.success();
    }

    @Operation(summary = "查询验收任务的测试记录")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/test-records")
    public Result<List<AcceptanceTestRecordVO>> listTestRecords(@PathVariable Long id) {
        return Result.success(acceptanceTaskService.listTestRecords(id));
    }

    /* ============ Flowable 集成辅助方法（兜底，异常仅记录日志） ============ */

    /**
     * 启动 Flowable 验收流程实例，businessKey = 验收任务 ID。
     */
    private void startFlowableSafely(Long taskId) {
        if (taskId == null) {
            return;
        }
        try {
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                log.warn("[Flowable] 启动验收流程失败：当前用户上下文为空，taskId={}", taskId);
                return;
            }
            Map<String, Object> variables = new HashMap<>();
            variables.put(FlowableProcessService.VAR_INITIATOR, String.valueOf(userId));
            flowableProcessService.startProcess(PROCESS_KEY, String.valueOf(taskId), variables);
            log.info("[Flowable] 验收流程已启动：taskId={}, initiator={}", taskId, userId);
        } catch (Exception e) {
            log.error("[Flowable] 启动验收流程异常（不影响主流程）：taskId={}", taskId, e);
        }
    }

    /**
     * 完成 Flowable 当前活动任务，根据审批结果设置 approved 变量。
     *
     * <p>通过 businessKey 反查流程实例下所有活动任务，完成第一个。
     * 多实例会签场景下需多次调用本方法（每次完成一个实例）。</p>
     */
    private void completeFlowableTaskSafely(Long taskId, boolean approved, String remark) {
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
            log.info("[Flowable] 验收流程任务完成：taskId={}, flowableTaskId={}, approved={}",
                    taskId, task.getId(), approved);
        } catch (Exception e) {
            log.error("[Flowable] 完成验收流程任务异常（不影响主流程）：taskId={}", taskId, e);
        }
    }

    /**
     * 解析审批结果字符串为 approved 布尔值。
     *
     * <p>PASS / CONDITIONAL_PASS / APPROVED 视为通过；其它视为拒绝。</p>
     */
    private boolean isApproved(String result) {
        return "PASS".equalsIgnoreCase(result)
                || "CONDITIONAL_PASS".equalsIgnoreCase(result)
                || "APPROVED".equalsIgnoreCase(result);
    }
}
