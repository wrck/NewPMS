package com.vibe.system.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysRoleDTO;
import com.vibe.system.dto.SysRoleMenuDTO;
import com.vibe.system.dto.SysRoleQueryDTO;
import com.vibe.system.service.SysRoleService;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysRoleVO;
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
 * 角色管理 Controller
 *
 * @author vibe
 */
@Tag(name = "角色管理", description = "角色 CRUD、菜单权限分配、数据权限配置")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @Operation(summary = "分页查询角色列表")
    @PreAuthorize("@ss.hasPermi('system:role') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<SysRoleVO>> page(@ParameterObject SysRoleQueryDTO query) {
        return Result.success(sysRoleService.page(query));
    }

    @Operation(summary = "查询全部启用角色（下拉选）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/all")
    public Result<List<SysRoleVO>> listAll() {
        return Result.success(sysRoleService.listAll());
    }

    @Operation(summary = "新增角色")
    @OperationLog(module = "角色管理", type = "INSERT", description = "新增角色")
    @PreAuthorize("@ss.hasPermi('system:role') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysRoleDTO dto) {
        return Result.success(sysRoleService.create(dto));
    }

    @Operation(summary = "编辑角色")
    @OperationLog(module = "角色管理", type = "UPDATE", description = "编辑角色")
    @PreAuthorize("@ss.hasPermi('system:role') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysRoleDTO dto) {
        dto.setId(id);
        sysRoleService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @OperationLog(module = "角色管理", type = "DELETE", description = "删除角色")
    @PreAuthorize("@ss.hasPermi('system:role') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "角色详情（含菜单ID）")
    @PreAuthorize("@ss.hasPermi('system:role') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<SysRoleVO> detail(@PathVariable Long id) {
        return Result.success(sysRoleService.getDetail(id));
    }

    @Operation(summary = "分配角色菜单权限")
    @OperationLog(module = "角色管理", type = "UPDATE", description = "分配角色菜单")
    @PreAuthorize("@ss.hasPermi('system:role:assign') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody SysRoleMenuDTO dto) {
        sysRoleService.assignMenus(id, dto);
        return Result.success();
    }

    @Operation(summary = "查询角色关联菜单ID列表")
    @PreAuthorize("@ss.hasPermi('system:role') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> roleMenus(@PathVariable Long id) {
        return Result.success(sysRoleService.getRoleMenuIds(id));
    }

    @Operation(summary = "按用户ID查询角色列表")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-user/{userId}")
    public Result<List<RoleSimpleVO>> rolesByUser(@PathVariable Long userId) {
        return Result.success(sysRoleService.getRolesByUserId(userId));
    }
}
