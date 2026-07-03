package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ProjectIssueDTO;
import com.vibe.project.service.ProjectIssueService;
import com.vibe.project.vo.ProjectIssueVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目问题 Controller
 *
 * @author vibe
 */
@Tag(name = "项目问题", description = "问题登记、状态流转、超期查询")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/issues")
@RequiredArgsConstructor
public class ProjectIssueController {

    private final ProjectIssueService projectIssueService;

    @Operation(summary = "查询项目问题列表")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<List<ProjectIssueVO>> list(@PathVariable Long projectId) {
        return Result.success(projectIssueService.listByProjectId(projectId));
    }

    @Operation(summary = "问题详情")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<ProjectIssueVO> detail(@PathVariable Long projectId, @PathVariable Long id) {
        return Result.success(projectIssueService.getDetail(id));
    }

    @Operation(summary = "登记问题")
    @OperationLog(module = "项目问题", type = "INSERT", description = "登记问题")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@PathVariable Long projectId, @Valid @RequestBody ProjectIssueDTO dto) {
        dto.setProjectId(projectId);
        return Result.success(projectIssueService.create(dto));
    }

    @Operation(summary = "编辑问题")
    @OperationLog(module = "项目问题", type = "UPDATE", description = "编辑问题")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                               @Valid @RequestBody ProjectIssueDTO dto) {
        dto.setId(id);
        dto.setProjectId(projectId);
        projectIssueService.update(dto);
        return Result.success();
    }

    @Operation(summary = "问题状态流转（OPEN→PROCESSING→RESOLVED→CLOSED）")
    @OperationLog(module = "项目问题", type = "UPDATE", description = "问题状态流转")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public Result<Void> transition(@PathVariable Long projectId, @PathVariable Long id,
                                   @RequestParam String targetStatus) {
        projectIssueService.transition(id, targetStatus);
        return Result.success();
    }

    @Operation(summary = "删除问题")
    @OperationLog(module = "项目问题", type = "DELETE", description = "删除问题")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        projectIssueService.delete(id);
        return Result.success();
    }
}
