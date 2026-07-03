package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ProjectMilestoneDTO;
import com.vibe.project.service.ProjectMilestoneService;
import com.vibe.project.vo.ProjectMilestoneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 项目里程碑 Controller
 *
 * @author vibe
 */
@Tag(name = "项目里程碑", description = "里程碑增删改查、标记达成")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/milestones")
@RequiredArgsConstructor
public class ProjectMilestoneController {

    private final ProjectMilestoneService projectMilestoneService;

    @Operation(summary = "查询项目里程碑列表")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<List<ProjectMilestoneVO>> list(@PathVariable Long projectId) {
        return Result.success(projectMilestoneService.listByProjectId(projectId));
    }

    @Operation(summary = "里程碑详情")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<ProjectMilestoneVO> detail(@PathVariable Long projectId, @PathVariable Long id) {
        return Result.success(projectMilestoneService.getDetail(id));
    }

    @Operation(summary = "新增里程碑")
    @OperationLog(module = "项目里程碑", type = "INSERT", description = "新增里程碑")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@PathVariable Long projectId, @Valid @RequestBody ProjectMilestoneDTO dto) {
        dto.setProjectId(projectId);
        return Result.success(projectMilestoneService.create(dto));
    }

    @Operation(summary = "编辑里程碑")
    @OperationLog(module = "项目里程碑", type = "UPDATE", description = "编辑里程碑")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                               @Valid @RequestBody ProjectMilestoneDTO dto) {
        dto.setId(id);
        dto.setProjectId(projectId);
        projectMilestoneService.update(dto);
        return Result.success();
    }

    @Operation(summary = "标记里程碑达成")
    @OperationLog(module = "项目里程碑", type = "UPDATE", description = "标记里程碑达成")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/reached")
    public Result<Void> markReached(@PathVariable Long projectId, @PathVariable Long id,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualDate) {
        projectMilestoneService.markReached(id, actualDate);
        return Result.success();
    }

    @Operation(summary = "删除里程碑")
    @OperationLog(module = "项目里程碑", type = "DELETE", description = "删除里程碑")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        projectMilestoneService.delete(id);
        return Result.success();
    }
}
