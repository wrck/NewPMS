package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.project.service.ProjectMemberService;
import com.vibe.project.vo.ProjectMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目成员 Controller
 *
 * <p>成员管理：查询、添加、改角色、移除。立项时 PM 自动加入成员。</p>
 *
 * @author vibe
 */
@Tag(name = "项目成员", description = "项目成员管理")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @Operation(summary = "查询项目成员列表")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<List<ProjectMemberVO>> list(@PathVariable Long projectId) {
        return Result.success(projectMemberService.listByProjectId(projectId));
    }

    @Operation(summary = "添加项目成员（重复加入幂等）")
    @OperationLog(module = "项目成员", type = "INSERT", description = "添加项目成员")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> addMember(@PathVariable Long projectId,
                                  @RequestParam Long userId,
                                  @RequestParam String role) {
        return Result.success(projectMemberService.addMember(projectId, userId, role));
    }

    @Operation(summary = "修改成员角色")
    @OperationLog(module = "项目成员", type = "UPDATE", description = "修改成员角色")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long projectId, @PathVariable Long id,
                                   @RequestParam String role) {
        projectMemberService.updateRole(id, role);
        return Result.success();
    }

    @Operation(summary = "移除项目成员")
    @OperationLog(module = "项目成员", type = "DELETE", description = "移除项目成员")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long projectId, @PathVariable Long id) {
        projectMemberService.remove(id);
        return Result.success();
    }
}
