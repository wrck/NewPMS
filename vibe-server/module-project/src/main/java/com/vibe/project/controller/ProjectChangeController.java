package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ChangeApproveDTO;
import com.vibe.project.dto.ProjectChangeDTO;
import com.vibe.project.service.ProjectChangeLogService;
import com.vibe.project.vo.ProjectChangeLogVO;
import com.vibe.service.FlowableProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
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
 * 项目变更 Controller
 *
 * <p>变更流程：申请（PENDING）→ 审批（APPROVED/REJECTED）→ 执行（EXECUTED）。</p>
 *
 * <p><b>Flowable 集成（增量增强）：</b>
 * <ul>
 *   <li>{@link #apply}：PM 发起变更后启动 Flowable {@code change} 流程</li>
 *   <li>{@link #approve}：总监审批，完成 Flowable 的"总监审批"任务（approved=true/false）</li>
 * </ul>
 * Flowable 操作采用 try/catch 兜底，若引擎异常不影响原状态机流转。</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "项目变更", description = "变更申请、影响评估、审批、执行")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/changes")
@RequiredArgsConstructor
public class ProjectChangeController {

    /** Flowable 流程定义 key：项目变更流程 */
    private static final String PROCESS_KEY = "change";

    private final ProjectChangeLogService projectChangeLogService;
    private final FlowableProcessService flowableProcessService;

    @Operation(summary = "查询项目变更记录（按时间倒序）")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<List<ProjectChangeLogVO>> list(@PathVariable Long projectId) {
        return Result.success(projectChangeLogService.listByProjectId(projectId));
    }

    @Operation(summary = "变更详情")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<ProjectChangeLogVO> detail(@PathVariable Long projectId, @PathVariable Long id) {
        return Result.success(projectChangeLogService.getDetail(id));
    }

    @Operation(summary = "提交变更申请（含影响评估）")
    @OperationLog(module = "项目变更", type = "INSERT", description = "提交变更申请")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> apply(@PathVariable Long projectId, @Valid @RequestBody ProjectChangeDTO dto) {
        dto.setProjectId(projectId);
        Long changeId = projectChangeLogService.applyChange(dto);
        // Flowable 增强：启动项目变更流程（异常不阻断主流程）
        startFlowableSafely(changeId);
        return Result.success(changeId);
    }

    @Operation(summary = "审批变更（APPROVED/REJECTED）")
    @OperationLog(module = "项目变更", type = "UPDATE", description = "审批变更")
    @PreAuthorize("@ss.hasPermi('project:change:approve') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long projectId, @PathVariable Long id,
                                @Valid @RequestBody ChangeApproveDTO dto) {
        projectChangeLogService.approve(id, dto);
        // Flowable 增强：完成"总监审批"任务
        boolean approved = "APPROVED".equalsIgnoreCase(dto.getApproveResult());
        completeFlowableTaskSafely(id, approved, dto.getOpinion());
        return Result.success();
    }

    @Operation(summary = "执行变更（APPROVED → EXECUTED）")
    @OperationLog(module = "项目变更", type = "UPDATE", description = "执行变更")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/execute")
    public Result<Void> execute(@PathVariable Long projectId, @PathVariable Long id) {
        projectChangeLogService.execute(id);
        return Result.success();
    }

    @Operation(summary = "删除变更记录")
    @OperationLog(module = "项目变更", type = "DELETE", description = "删除变更记录")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        projectChangeLogService.delete(id);
        return Result.success();
    }

    /* ============ Flowable 集成辅助方法（兜底，异常仅记录日志） ============ */

    private void startFlowableSafely(Long changeId) {
        if (changeId == null) {
            return;
        }
        try {
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                log.warn("[Flowable] 启动变更流程失败：当前用户上下文为空，changeId={}", changeId);
                return;
            }
            Map<String, Object> variables = new HashMap<>();
            variables.put(FlowableProcessService.VAR_INITIATOR, String.valueOf(userId));
            flowableProcessService.startProcess(PROCESS_KEY, String.valueOf(changeId), variables);
            log.info("[Flowable] 变更流程已启动：changeId={}, initiator={}", changeId, userId);
        } catch (Exception e) {
            log.error("[Flowable] 启动变更流程异常（不影响主流程）：changeId={}", changeId, e);
        }
    }

    private void completeFlowableTaskSafely(Long changeId, boolean approved, String opinion) {
        if (changeId == null) {
            return;
        }
        try {
            List<Task> activeTasks = flowableProcessService.findActiveTasksByBusinessKey(
                    PROCESS_KEY, String.valueOf(changeId));
            if (activeTasks.isEmpty()) {
                log.warn("[Flowable] 未找到活动任务：changeId={}, processKey={}", changeId, PROCESS_KEY);
                return;
            }
            Task task = activeTasks.get(0);
            Map<String, Object> variables = new HashMap<>();
            if (opinion != null) {
                variables.put("opinion", opinion);
            }
            if (approved) {
                flowableProcessService.approve(task.getId(), variables);
            } else {
                flowableProcessService.reject(task.getId(), opinion);
            }
            log.info("[Flowable] 变更流程任务完成：changeId={}, flowableTaskId={}, approved={}",
                    changeId, task.getId(), approved);
        } catch (Exception e) {
            log.error("[Flowable] 完成变更流程任务异常（不影响主流程）：changeId={}", changeId, e);
        }
    }
}
