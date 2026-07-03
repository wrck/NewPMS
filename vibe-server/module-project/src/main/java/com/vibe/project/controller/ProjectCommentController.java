package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ProjectCommentDTO;
import com.vibe.project.service.ProjectCommentService;
import com.vibe.project.vo.ProjectCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目沟通记录 Controller
 *
 * <p>支持项目维度与任务维度的评论查询，评论支持回复（parentId）。</p>
 *
 * @author vibe
 */
@Tag(name = "项目沟通", description = "项目/任务评论、回复")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/comments")
@RequiredArgsConstructor
public class ProjectCommentController {

    private final ProjectCommentService projectCommentService;

    @Operation(summary = "查询项目评论列表（含回复树结构）")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<List<ProjectCommentVO>> listByProject(@PathVariable Long projectId) {
        return Result.success(projectCommentService.listByProjectId(projectId));
    }

    @Operation(summary = "查询任务评论列表")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/tasks/{taskId}")
    public Result<List<ProjectCommentVO>> listByTask(@PathVariable Long projectId, @PathVariable Long taskId) {
        return Result.success(projectCommentService.listByTaskId(taskId));
    }

    @Operation(summary = "发表评论（自动填充当前登录人为作者，支持回复 parentId）")
    @OperationLog(module = "项目沟通", type = "INSERT", description = "发表评论")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public Result<Long> create(@PathVariable Long projectId, @Valid @RequestBody ProjectCommentDTO dto) {
        dto.setProjectId(projectId);
        return Result.success(projectCommentService.create(dto));
    }

    @Operation(summary = "删除评论（仅作者或管理员可删除）")
    @OperationLog(module = "项目沟通", type = "DELETE", description = "删除评论")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        projectCommentService.delete(id);
        return Result.success();
    }
}
