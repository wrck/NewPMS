package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.project.dto.ProjectRiskDTO;
import com.vibe.project.service.ProjectRiskService;
import com.vibe.project.vo.ProjectRiskVO;
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
 * 项目风险 Controller
 *
 * @author vibe
 */
@Tag(name = "项目风险", description = "风险登记、状态流转、超期查询")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/risks")
@RequiredArgsConstructor
public class ProjectRiskController {

    private final ProjectRiskService projectRiskService;

    @Operation(summary = "查询项目风险列表")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<List<ProjectRiskVO>> list(@PathVariable Long projectId) {
        return Result.success(projectRiskService.listByProjectId(projectId));
    }

    @Operation(summary = "风险详情")
    @PreAuthorize("@ss.hasPermi('project:project') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<ProjectRiskVO> detail(@PathVariable Long projectId, @PathVariable Long id) {
        return Result.success(projectRiskService.getDetail(id));
    }

    @Operation(summary = "登记风险")
    @OperationLog(module = "项目风险", type = "INSERT", description = "登记风险")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@PathVariable Long projectId, @Valid @RequestBody ProjectRiskDTO dto) {
        dto.setProjectId(projectId);
        return Result.success(projectRiskService.create(dto));
    }

    @Operation(summary = "编辑风险")
    @OperationLog(module = "项目风险", type = "UPDATE", description = "编辑风险")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long projectId, @PathVariable Long id,
                               @Valid @RequestBody ProjectRiskDTO dto) {
        dto.setId(id);
        dto.setProjectId(projectId);
        projectRiskService.update(dto);
        return Result.success();
    }

    @Operation(summary = "风险状态流转（OPEN→PROCESSING→RESOLVED→CLOSED）")
    @OperationLog(module = "项目风险", type = "UPDATE", description = "风险状态流转")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public Result<Void> transition(@PathVariable Long projectId, @PathVariable Long id,
                                   @RequestParam String targetStatus) {
        projectRiskService.transition(id, targetStatus);
        return Result.success();
    }

    @Operation(summary = "删除风险")
    @OperationLog(module = "项目风险", type = "DELETE", description = "删除风险")
    @PreAuthorize("@ss.hasPermi('project:project:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        projectRiskService.delete(id);
        return Result.success();
    }
}
