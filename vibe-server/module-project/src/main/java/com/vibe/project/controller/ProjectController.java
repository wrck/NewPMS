package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ProjectArchiveDTO;
import com.vibe.project.dto.ProjectCreateDTO;
import com.vibe.project.dto.ProjectQueryDTO;
import com.vibe.project.dto.ProjectStatusDTO;
import com.vibe.project.dto.ProjectUpdateDTO;
import com.vibe.project.service.ProjectService;
import com.vibe.project.vo.ProjectDetailVO;
import com.vibe.project.vo.ProjectGanttVO;
import com.vibe.project.vo.ProjectKanbanVO;
import com.vibe.project.vo.ProjectVO;
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

import java.util.List;

/**
 * 项目管理 Controller
 *
 * <p>核心能力：立项 / 编辑删除 / 状态流转 / 分页查询 / 看板分组 /
 * 甘特图 / 详情聚合 / 结项检查 / 归档。</p>
 *
 * @author vibe
 */
@Tag(name = "项目管理", description = "项目立项、状态机、看板、甘特图、结项归档")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "分页查询项目")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<ProjectVO>> page(@ParameterObject ProjectQueryDTO query) {
        return Result.success(projectService.page(query));
    }

    @Operation(summary = "项目详情聚合（含阶段、里程碑、任务统计、风险问题计数）")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<ProjectDetailVO> detail(@PathVariable Long id) {
        return Result.success(projectService.getDetail(id));
    }

    @Operation(summary = "立项（手动创建 / 选择模板生成阶段与任务）")
    @OperationLog(module = "项目管理", type = "INSERT", description = "项目立项")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProjectCreateDTO dto) {
        return Result.success(projectService.create(dto));
    }

    @Operation(summary = "编辑项目")
    @OperationLog(module = "项目管理", type = "UPDATE", description = "编辑项目")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateDTO dto) {
        dto.setId(id);
        projectService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除项目（仅 INIT/PLAN 状态可删除）")
    @OperationLog(module = "项目管理", type = "DELETE", description = "删除项目")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.success();
    }

    @Operation(summary = "项目状态流转（含乐观锁校验）")
    @OperationLog(module = "项目管理", type = "UPDATE", description = "项目状态流转")
    @PreAuthorize("@ss.hasPermi('project:project:flow') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public Result<Void> transition(@PathVariable Long id, @Valid @RequestBody ProjectStatusDTO dto) {
        dto.setId(id);
        projectService.transition(dto);
        return Result.success();
    }

    @Operation(summary = "看板分组查询（按状态分组）")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/kanban")
    public Result<List<ProjectKanbanVO>> kanban(@ParameterObject ProjectQueryDTO query) {
        return Result.success(projectService.kanban(query));
    }

    @Operation(summary = "甘特图数据")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}/gantt")
    public Result<ProjectGanttVO> gantt(@PathVariable Long id) {
        return Result.success(projectService.gantt(id));
    }

    @Operation(summary = "结项检查（返回不满足原因，null 表示通过）")
    @PreAuthorize("@ss.hasPermi('project:project:flow') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}/close-check")
    public Result<String> checkClose(@PathVariable Long id) {
        return Result.success(projectService.checkClose(id));
    }

    @Operation(summary = "归档（CLOSE → ARCHIVED，含复盘记录）")
    @OperationLog(module = "项目管理", type = "UPDATE", description = "项目归档")
    @PreAuthorize("@ss.hasPermi('project:project:flow') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id, @RequestBody ProjectArchiveDTO dto) {
        projectService.archive(id, dto);
        return Result.success();
    }
}
