package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.project.dto.BatchTaskDispatchDTO;
import com.vibe.project.dto.ProjectTaskDTO;
import com.vibe.project.dto.ProjectTaskQueryDTO;
import com.vibe.project.dto.TaskDispatchDTO;
import com.vibe.project.dto.TaskProgressDTO;
import com.vibe.project.dto.TaskReturnDTO;
import com.vibe.project.dto.TaskTransferDTO;
import com.vibe.project.dto.export.ProjectTaskExportDTO;
import com.vibe.project.service.ProjectTaskService;
import com.vibe.project.vo.ProjectTaskVO;
import com.vibe.utils.ExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目任务 Controller
 *
 * <p>路径设计：</p>
 * <ul>
 *   <li>{@code /api/v1/projects/{projectId}/tasks} - 项目维度任务 CRUD</li>
 *   <li>{@code /api/v1/projects/tasks/{taskId}} - 任务维度操作（派发/转派/退回/进度/改期）</li>
 *   <li>{@code /api/v1/projects/tasks} - 任务分页查询（跨项目，含数据权限）</li>
 * </ul>
 *
 * @author vibe
 */
@Tag(name = "项目任务", description = "任务分解、派发、转派、退回、进度、甘特图排期")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectTaskController {

    private final ProjectTaskService projectTaskService;

    /** 单次导出最大行数 */
    private static final int EXPORT_MAX_ROWS = 10000;

    /* ============ 项目维度：任务 CRUD ============ */

    @Operation(summary = "分页查询任务（含数据权限过滤）")
    @PreAuthorize("@ss.hasPermi('project:task') or hasRole('SUPER_ADMIN')")
    @GetMapping("/tasks")
    public Result<PageResult<ProjectTaskVO>> page(@ParameterObject ProjectTaskQueryDTO query) {
        return Result.success(projectTaskService.page(query));
    }

    @Operation(summary = "查询项目下的全部任务")
    @PreAuthorize("@ss.hasPermi('project:task') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{projectId}/tasks")
    public Result<List<ProjectTaskVO>> list(@PathVariable Long projectId) {
        return Result.success(projectTaskService.listByProjectId(projectId));
    }

    @Operation(summary = "任务详情")
    @PreAuthorize("@ss.hasPermi('project:task') or hasRole('SUPER_ADMIN')")
    @GetMapping("/tasks/{taskId}")
    public Result<ProjectTaskVO> detail(@PathVariable Long taskId) {
        return Result.success(projectTaskService.getDetail(taskId));
    }

    @Operation(summary = "新增任务（支持父子任务分解）")
    @OperationLog(module = "项目任务", type = "INSERT", description = "新增任务")
    @PreAuthorize("@ss.hasPermi('project:task:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/{projectId}/tasks")
    public Result<Long> create(@PathVariable Long projectId, @Valid @RequestBody ProjectTaskDTO dto) {
        dto.setProjectId(projectId);
        return Result.success(projectTaskService.create(dto));
    }

    @Operation(summary = "编辑任务")
    @OperationLog(module = "项目任务", type = "UPDATE", description = "编辑任务")
    @PreAuthorize("@ss.hasPermi('project:task:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/tasks/{taskId}")
    public Result<Void> update(@PathVariable Long taskId, @Valid @RequestBody ProjectTaskDTO dto) {
        dto.setId(taskId);
        projectTaskService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除任务（存在子任务时不允许删除）")
    @OperationLog(module = "项目任务", type = "DELETE", description = "删除任务")
    @PreAuthorize("@ss.hasPermi('project:task:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/tasks/{taskId}")
    public Result<Void> delete(@PathVariable Long taskId) {
        projectTaskService.delete(taskId);
        return Result.success();
    }

    /* ============ 任务操作：派发 / 批量派单 / 转派 / 退回 / 进度 / 改期 ============ */

    @Operation(summary = "任务派发（SELF→assigneeId / AGENT→agentCompanyId）")
    @OperationLog(module = "项目任务", type = "UPDATE", description = "任务派发")
    @PreAuthorize("@ss.hasPermi('project:task:dispatch') or hasRole('SUPER_ADMIN')")
    @PutMapping("/tasks/{taskId}/dispatch")
    public Result<Void> dispatch(@PathVariable Long taskId, @Valid @RequestBody TaskDispatchDTO dto) {
        projectTaskService.dispatch(taskId, dto);
        return Result.success();
    }

    @Operation(summary = "批量派单")
    @OperationLog(module = "项目任务", type = "UPDATE", description = "批量派单")
    @PreAuthorize("@ss.hasPermi('project:task:dispatch') or hasRole('SUPER_ADMIN')")
    @PostMapping("/tasks/batch-dispatch")
    public Result<Integer> batchDispatch(@Valid @RequestBody BatchTaskDispatchDTO dto) {
        return Result.success(projectTaskService.batchDispatch(dto));
    }

    @Operation(summary = "任务转派")
    @OperationLog(module = "项目任务", type = "UPDATE", description = "任务转派")
    @PreAuthorize("@ss.hasPermi('project:task:dispatch') or hasRole('SUPER_ADMIN')")
    @PutMapping("/tasks/{taskId}/transfer")
    public Result<Void> transfer(@PathVariable Long taskId, @Valid @RequestBody TaskTransferDTO dto) {
        projectTaskService.transfer(taskId, dto);
        return Result.success();
    }

    @Operation(summary = "任务退回（清空执行人，回到 PENDING）")
    @OperationLog(module = "项目任务", type = "UPDATE", description = "任务退回")
    @PreAuthorize("@ss.hasPermi('project:task:dispatch') or hasRole('SUPER_ADMIN')")
    @PutMapping("/tasks/{taskId}/return")
    public Result<Void> returnTask(@PathVariable Long taskId, @RequestBody TaskReturnDTO dto) {
        projectTaskService.returnTask(taskId, dto);
        return Result.success();
    }

    @Operation(summary = "进度更新（状态流转，含乐观锁校验）")
    @OperationLog(module = "项目任务", type = "UPDATE", description = "任务进度更新")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/tasks/{taskId}/progress")
    public Result<Void> updateProgress(@PathVariable Long taskId, @Valid @RequestBody TaskProgressDTO dto) {
        projectTaskService.updateProgress(taskId, dto);
        return Result.success();
    }

    @Operation(summary = "甘特图拖拽排期（含依赖冲突检测）")
    @OperationLog(module = "项目任务", type = "UPDATE", description = "任务改期")
    @PreAuthorize("@ss.hasPermi('project:task:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/tasks/{taskId}/reschedule")
    public Result<Void> reschedule(@PathVariable Long taskId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newStart,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newEnd) {
        projectTaskService.reschedule(taskId, newStart, newEnd);
        return Result.success();
    }

    /* ============ 查询：进度预警 ============ */

    @Operation(summary = "进度预警：超期未完成任务列表")
    @PreAuthorize("@ss.hasPermi('project:task') or hasRole('SUPER_ADMIN')")
    @GetMapping("/tasks/overdue")
    public Result<List<ProjectTaskVO>> overdueTasks() {
        return Result.success(projectTaskService.listOverdueTasks());
    }

    @Operation(summary = "导出任务列表（Excel）")
    @OperationLog(module = "项目任务", type = "EXPORT", description = "导出任务列表")
    @PreAuthorize("@ss.hasPermi('project:task') or hasRole('SUPER_ADMIN')")
    @GetMapping("/tasks/export")
    public void export(HttpServletResponse response, @ParameterObject ProjectTaskQueryDTO query) throws IOException {
        query.setPage(1);
        query.setSize(EXPORT_MAX_ROWS);
        List<ProjectTaskVO> records = projectTaskService.page(query).getRecords();
        List<ProjectTaskExportDTO> data = records.stream().map(vo -> {
            ProjectTaskExportDTO dto = new ProjectTaskExportDTO();
            BeanUtils.copyProperties(vo, dto);
            return dto;
        }).collect(Collectors.toList());
        ExcelUtils.export(response, "项目任务列表", "任务", ProjectTaskExportDTO.class, data);
    }
}
