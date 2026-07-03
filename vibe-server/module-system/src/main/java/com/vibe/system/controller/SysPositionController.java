package com.vibe.system.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysPositionDTO;
import com.vibe.system.service.SysPositionService;
import com.vibe.system.vo.SysPositionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * 岗位 Controller
 *
 * @author vibe
 */
@Tag(name = "岗位管理", description = "岗位 CRUD")
@RestController
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
public class SysPositionController {

    private final SysPositionService sysPositionService;

    @Operation(summary = "分页查询岗位")
    @PreAuthorize("@ss.hasPermi('system:org') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<SysPositionVO>> page(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "组织ID") @RequestParam(required = false) Long orgId) {
        return Result.success(sysPositionService.page(page, size, keyword, orgId));
    }

    @Operation(summary = "新增岗位")
    @OperationLog(module = "岗位管理", type = "INSERT", description = "新增岗位")
    @PreAuthorize("@ss.hasPermi('system:org') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysPositionDTO dto) {
        return Result.success(sysPositionService.create(dto));
    }

    @Operation(summary = "编辑岗位")
    @OperationLog(module = "岗位管理", type = "UPDATE", description = "编辑岗位")
    @PreAuthorize("@ss.hasPermi('system:org') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysPositionDTO dto) {
        dto.setId(id);
        sysPositionService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除岗位")
    @OperationLog(module = "岗位管理", type = "DELETE", description = "删除岗位")
    @PreAuthorize("@ss.hasPermi('system:org') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysPositionService.delete(id);
        return Result.success();
    }

    @Operation(summary = "岗位详情")
    @PreAuthorize("@ss.hasPermi('system:org') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<SysPositionVO> detail(@PathVariable Long id) {
        return Result.success(sysPositionService.getDetail(id));
    }
}
