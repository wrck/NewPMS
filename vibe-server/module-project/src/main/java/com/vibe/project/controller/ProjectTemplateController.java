package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ProjectTemplateDTO;
import com.vibe.project.dto.ProjectTemplatePhaseDTO;
import com.vibe.project.dto.ProjectTemplateQueryDTO;
import com.vibe.project.dto.ProjectTemplateTaskDTO;
import com.vibe.project.service.ProjectTemplateService;
import com.vibe.project.vo.ProjectTemplateDetailVO;
import com.vibe.project.vo.ProjectTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/**
 * 项目模板 Controller
 *
 * <p>模板用于立项时一键生成项目阶段与任务。模板 CRUD + 阶段/任务 CRUD。</p>
 *
 * @author vibe
 */
@Tag(name = "项目模板", description = "模板 CRUD、阶段任务管理")
@RestController
@RequestMapping("/api/v1/project-templates")
@RequiredArgsConstructor
public class ProjectTemplateController {

    private final ProjectTemplateService projectTemplateService;

    @Operation(summary = "分页查询项目模板")
    @PreAuthorize("@ss.hasPermi('project:template') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<ProjectTemplateVO>> page(@ParameterObject ProjectTemplateQueryDTO query) {
        return Result.success(projectTemplateService.page(query));
    }

    @Operation(summary = "模板详情（含阶段与任务）")
    @PreAuthorize("@ss.hasPermi('project:template') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<ProjectTemplateDetailVO> detail(@PathVariable Long id) {
        return Result.success(projectTemplateService.getDetail(id));
    }

    @Operation(summary = "新增模板")
    @OperationLog(module = "项目模板", type = "INSERT", description = "新增项目模板")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProjectTemplateDTO dto) {
        return Result.success(projectTemplateService.create(dto));
    }

    @Operation(summary = "编辑模板基础信息")
    @OperationLog(module = "项目模板", type = "UPDATE", description = "编辑项目模板")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectTemplateDTO dto) {
        dto.setId(id);
        projectTemplateService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除模板（连同阶段与任务逻辑删除）")
    @OperationLog(module = "项目模板", type = "DELETE", description = "删除项目模板")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectTemplateService.delete(id);
        return Result.success();
    }

    /* ============ 模板阶段 ============ */

    @Operation(summary = "新增模板阶段")
    @OperationLog(module = "项目模板", type = "INSERT", description = "新增模板阶段")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/{templateId}/phases")
    public Result<Long> addPhase(@PathVariable Long templateId,
                                 @Valid @RequestBody ProjectTemplatePhaseDTO dto) {
        dto.setTemplateId(templateId);
        return Result.success(projectTemplateService.addPhase(dto));
    }

    @Operation(summary = "编辑模板阶段")
    @OperationLog(module = "项目模板", type = "UPDATE", description = "编辑模板阶段")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/phases/{id}")
    public Result<Void> updatePhase(@PathVariable Long id, @Valid @RequestBody ProjectTemplatePhaseDTO dto) {
        dto.setId(id);
        projectTemplateService.updatePhase(dto);
        return Result.success();
    }

    @Operation(summary = "删除模板阶段")
    @OperationLog(module = "项目模板", type = "DELETE", description = "删除模板阶段")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/phases/{id}")
    public Result<Void> deletePhase(@PathVariable Long id) {
        projectTemplateService.deletePhase(id);
        return Result.success();
    }

    /* ============ 模板任务 ============ */

    @Operation(summary = "新增模板任务")
    @OperationLog(module = "项目模板", type = "INSERT", description = "新增模板任务")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/{templateId}/tasks")
    public Result<Long> addTask(@PathVariable Long templateId,
                                @Valid @RequestBody ProjectTemplateTaskDTO dto) {
        dto.setTemplateId(templateId);
        return Result.success(projectTemplateService.addTask(dto));
    }

    @Operation(summary = "编辑模板任务")
    @OperationLog(module = "项目模板", type = "UPDATE", description = "编辑模板任务")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/tasks/{id}")
    public Result<Void> updateTask(@PathVariable Long id, @Valid @RequestBody ProjectTemplateTaskDTO dto) {
        dto.setId(id);
        projectTemplateService.updateTask(dto);
        return Result.success();
    }

    @Operation(summary = "删除模板任务")
    @OperationLog(module = "项目模板", type = "DELETE", description = "删除模板任务")
    @PreAuthorize("@ss.hasPermi('project:template:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        projectTemplateService.deleteTask(id);
        return Result.success();
    }
}
