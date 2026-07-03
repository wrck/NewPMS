package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ProjectPhaseDTO;
import com.vibe.project.service.ProjectPhaseService;
import com.vibe.project.vo.ProjectPhaseVO;
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
 * 项目阶段 Controller
 *
 * @author vibe
 */
@Tag(name = "项目阶段", description = "项目阶段增删改查、时间范围、交付物清单")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/phases")
@RequiredArgsConstructor
public class ProjectPhaseController {

    private final ProjectPhaseService projectPhaseService;

    @Operation(summary = "查询项目下的全部阶段")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<List<ProjectPhaseVO>> list(@PathVariable Long projectId) {
        return Result.success(projectPhaseService.listByProjectId(projectId));
    }

    @Operation(summary = "阶段详情")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<ProjectPhaseVO> detail(@PathVariable Long projectId, @PathVariable Long id) {
        return Result.success(projectPhaseService.getDetail(id));
    }

    @Operation(summary = "新增阶段")
    @OperationLog(module = "项目阶段", type = "INSERT", description = "新增项目阶段")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@PathVariable Long projectId, @Valid @RequestBody ProjectPhaseDTO dto) {
        dto.setProjectId(projectId);
        return Result.success(projectPhaseService.create(dto));
    }

    @Operation(summary = "编辑阶段")
    @OperationLog(module = "项目阶段", type = "UPDATE", description = "编辑项目阶段")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                               @Valid @RequestBody ProjectPhaseDTO dto) {
        dto.setId(id);
        dto.setProjectId(projectId);
        projectPhaseService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除阶段")
    @OperationLog(module = "项目阶段", type = "DELETE", description = "删除项目阶段")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        projectPhaseService.delete(id);
        return Result.success();
    }
}
