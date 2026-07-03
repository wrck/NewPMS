package com.vibe.system.controller;

import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysOrgDTO;
import com.vibe.system.service.SysOrgService;
import com.vibe.system.vo.SysOrgVO;
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
 * 组织架构 Controller
 *
 * @author vibe
 */
@Tag(name = "组织架构", description = "组织树 CRUD")
@RestController
@RequestMapping("/api/v1/orgs")
@RequiredArgsConstructor
public class SysOrgController {

    private final SysOrgService sysOrgService;

    @Operation(summary = "查询组织树")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tree")
    public Result<List<SysOrgVO>> tree() {
        return Result.success(sysOrgService.listTree());
    }

    @Operation(summary = "查询组织扁平列表")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<List<SysOrgVO>> list() {
        return Result.success(sysOrgService.listAll());
    }

    @Operation(summary = "新增组织")
    @OperationLog(module = "组织架构", type = "INSERT", description = "新增组织")
    @PreAuthorize("@ss.hasPermi('system:org') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysOrgDTO dto) {
        return Result.success(sysOrgService.create(dto));
    }

    @Operation(summary = "编辑组织")
    @OperationLog(module = "组织架构", type = "UPDATE", description = "编辑组织")
    @PreAuthorize("@ss.hasPermi('system:org') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysOrgDTO dto) {
        dto.setId(id);
        sysOrgService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除组织")
    @OperationLog(module = "组织架构", type = "DELETE", description = "删除组织")
    @PreAuthorize("@ss.hasPermi('system:org') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysOrgService.delete(id);
        return Result.success();
    }

    @Operation(summary = "组织详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<SysOrgVO> detail(@PathVariable Long id) {
        return Result.success(sysOrgService.getDetail(id));
    }
}
