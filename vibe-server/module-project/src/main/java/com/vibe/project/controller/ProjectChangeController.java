package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ChangeApproveDTO;
import com.vibe.project.dto.ProjectChangeDTO;
import com.vibe.project.service.ProjectChangeLogService;
import com.vibe.project.vo.ProjectChangeLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目变更 Controller
 *
 * <p>变更流程：申请（PENDING）→ 审批（APPROVED/REJECTED）→ 执行（EXECUTED）。</p>
 *
 * @author vibe
 */
@Tag(name = "项目变更", description = "变更申请、影响评估、审批、执行")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/changes")
@RequiredArgsConstructor
public class ProjectChangeController {

    private final ProjectChangeLogService projectChangeLogService;

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
        return Result.success(projectChangeLogService.applyChange(dto));
    }

    @Operation(summary = "审批变更（APPROVED/REJECTED）")
    @OperationLog(module = "项目变更", type = "UPDATE", description = "审批变更")
    @PreAuthorize("@ss.hasPermi('project:change:approve') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long projectId, @PathVariable Long id,
                                @Valid @RequestBody ChangeApproveDTO dto) {
        projectChangeLogService.approve(id, dto);
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
}
