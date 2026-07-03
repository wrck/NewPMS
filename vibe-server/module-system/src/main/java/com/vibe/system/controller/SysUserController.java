package com.vibe.system.controller;

import com.vibe.common.context.UserContextHolder;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysUserDTO;
import com.vibe.system.dto.SysUserPasswordDTO;
import com.vibe.system.dto.SysUserQueryDTO;
import com.vibe.system.dto.SysUserStatusDTO;
import com.vibe.system.dto.SysUserRoleDTO;
import com.vibe.system.service.SysUserService;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysUserVO;
import com.vibe.system.vo.UserInfoVO;
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
 * 用户管理 Controller
 *
 * @author vibe
 */
@Tag(name = "用户管理", description = "系统用户 CRUD、角色分配、状态管理、密码重置")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询用户列表")
    @OperationLog(module = "用户管理", type = "QUERY", description = "分页查询用户")
    @PreAuthorize("@ss.hasPermi('system:user') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<SysUserVO>> page(@ParameterObject SysUserQueryDTO query) {
        return Result.success(sysUserService.page(query));
    }

    @Operation(summary = "新增用户")
    @OperationLog(module = "用户管理", type = "INSERT", description = "新增用户")
    @PreAuthorize("@ss.hasPermi('system:user:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysUserDTO dto) {
        return Result.success(sysUserService.create(dto));
    }

    @Operation(summary = "编辑用户")
    @OperationLog(module = "用户管理", type = "UPDATE", description = "编辑用户")
    @PreAuthorize("@ss.hasPermi('system:user:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysUserDTO dto) {
        dto.setId(id);
        sysUserService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @OperationLog(module = "用户管理", type = "DELETE", description = "删除用户")
    @PreAuthorize("@ss.hasPermi('system:user:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }

    @Operation(summary = "用户详情（含角色）")
    @PreAuthorize("@ss.hasPermi('system:user') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<SysUserVO> detail(@PathVariable Long id) {
        return Result.success(sysUserService.getDetail(id));
    }

    @Operation(summary = "分配用户角色")
    @OperationLog(module = "用户管理", type = "UPDATE", description = "分配用户角色")
    @PreAuthorize("@ss.hasPermi('system:role:assign') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody SysUserRoleDTO dto) {
        sysUserService.assignRoles(id, dto);
        return Result.success();
    }

    @Operation(summary = "查询用户角色列表")
    @PreAuthorize("@ss.hasPermi('system:user') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}/roles")
    public Result<List<RoleSimpleVO>> userRoles(@PathVariable Long id) {
        return Result.success(sysUserService.getUserRoles(id));
    }

    @Operation(summary = "变更用户状态")
    @OperationLog(module = "用户管理", type = "UPDATE", description = "变更用户状态")
    @PreAuthorize("@ss.hasPermi('system:user:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody SysUserStatusDTO dto) {
        sysUserService.changeStatus(id, dto);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @OperationLog(module = "用户管理", type = "UPDATE", description = "重置用户密码")
    @PreAuthorize("@ss.hasPermi('system:user:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody SysUserPasswordDTO dto) {
        sysUserService.resetPassword(id, dto);
        return Result.success();
    }

    @Operation(summary = "当前登录用户信息（含角色权限）")
    @GetMapping("/info")
    public Result<UserInfoVO> currentUserInfo() {
        return Result.success(sysUserService.getCurrentUserInfo());
    }

    @Operation(summary = "当前登录用户ID（调试用）")
    @GetMapping("/me")
    public Result<Long> currentUserId() {
        return Result.success(UserContextHolder.getUserId());
    }
}
