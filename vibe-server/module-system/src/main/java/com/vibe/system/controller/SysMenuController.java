package com.vibe.system.controller;

import com.vibe.common.context.UserContextHolder;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysMenuDTO;
import com.vibe.system.service.SysMenuService;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysMenuVO;
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
 * 菜单管理 Controller
 *
 * @author vibe
 */
@Tag(name = "菜单管理", description = "菜单/按钮权限 CRUD、菜单树、按角色查询菜单")
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @Operation(summary = "查询菜单树（全部）")
    @PreAuthorize("@ss.hasPermi('system:menu') or hasRole('SUPER_ADMIN')")
    @GetMapping("/tree")
    public Result<List<SysMenuVO>> tree() {
        return Result.success(sysMenuService.listTree());
    }

    @Operation(summary = "查询菜单扁平列表")
    @PreAuthorize("@ss.hasPermi('system:menu') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<List<SysMenuVO>> list() {
        return Result.success(sysMenuService.listAll());
    }

    @Operation(summary = "新增菜单")
    @OperationLog(module = "菜单管理", type = "INSERT", description = "新增菜单")
    @PreAuthorize("@ss.hasPermi('system:menu') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysMenuDTO dto) {
        return Result.success(sysMenuService.create(dto));
    }

    @Operation(summary = "编辑菜单")
    @OperationLog(module = "菜单管理", type = "UPDATE", description = "编辑菜单")
    @PreAuthorize("@ss.hasPermi('system:menu') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysMenuDTO dto) {
        dto.setId(id);
        sysMenuService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @OperationLog(module = "菜单管理", type = "DELETE", description = "删除菜单")
    @PreAuthorize("@ss.hasPermi('system:menu') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.delete(id);
        return Result.success();
    }

    @Operation(summary = "菜单详情")
    @PreAuthorize("@ss.hasPermi('system:menu') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<SysMenuVO> detail(@PathVariable Long id) {
        return Result.success(sysMenuService.getDetail(id));
    }

    @Operation(summary = "按角色查询菜单树")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-role/{roleId}")
    public Result<List<SysMenuVO>> menusByRole(@PathVariable Long roleId) {
        return Result.success(sysMenuService.getMenusByRoleId(roleId));
    }

    @Operation(summary = "当前登录用户的菜单树（前端动态路由）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-tree")
    public Result<List<SysMenuVO>> myMenuTree() {
        return Result.success(sysMenuService.getMenusByUserId(UserContextHolder.getUserId()));
    }

    @Operation(summary = "查询菜单关联的角色列表")
    @PreAuthorize("@ss.hasPermi('system:menu') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{menuId}/roles")
    public Result<List<RoleSimpleVO>> menuRoles(@PathVariable Long menuId) {
        return Result.success(sysMenuService.getRolesByMenuId(menuId));
    }

    @Operation(summary = "给菜单分配角色（全量覆盖）")
    @OperationLog(module = "菜单管理", type = "UPDATE", description = "给菜单分配角色")
    @PreAuthorize("@ss.hasPermi('system:menu') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{menuId}/roles")
    public Result<Void> assignRoles(@PathVariable Long menuId, @RequestBody MenuRoleAssignDTO dto) {
        sysMenuService.assignRolesToMenu(menuId, dto.getRoleIds());
        return Result.success();
    }

    /**
     * 菜单-角色分配入参
     */
    @lombok.Data
    public static class MenuRoleAssignDTO {
        private List<Long> roleIds;
    }
}
