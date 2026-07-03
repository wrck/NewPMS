package com.vibe.system.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysConfigDTO;
import com.vibe.system.dto.SysConfigQueryDTO;
import com.vibe.system.service.SysConfigService;
import com.vibe.system.vo.SysConfigVO;
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
 * 系统配置 Controller
 *
 * @author vibe
 */
@Tag(name = "系统配置", description = "系统配置 CRUD、按 key 查询（带缓存）")
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "分页查询系统配置")
    @PreAuthorize("@ss.hasPermi('system:config') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<SysConfigVO>> page(@ParameterObject SysConfigQueryDTO query) {
        return Result.success(sysConfigService.page(query));
    }

    @Operation(summary = "按 configKey 查询配置值（带缓存）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/key/{configKey}")
    public Result<String> getByKey(@PathVariable String configKey) {
        return Result.success(sysConfigService.getConfigValue(configKey));
    }

    @Operation(summary = "按 configKey 查询配置值（带缓存，兼容旧版路径）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-key/{configKey}")
    public Result<String> getByKeyLegacy(@PathVariable String configKey) {
        return Result.success(sysConfigService.getConfigValue(configKey));
    }

    @Operation(summary = "新增系统配置")
    @OperationLog(module = "系统配置", type = "INSERT", description = "新增配置")
    @PreAuthorize("@ss.hasPermi('system:config') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysConfigDTO dto) {
        return Result.success(sysConfigService.create(dto));
    }

    @Operation(summary = "编辑系统配置")
    @OperationLog(module = "系统配置", type = "UPDATE", description = "编辑配置")
    @PreAuthorize("@ss.hasPermi('system:config') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysConfigDTO dto) {
        dto.setId(id);
        sysConfigService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除系统配置")
    @OperationLog(module = "系统配置", type = "DELETE", description = "删除配置")
    @PreAuthorize("@ss.hasPermi('system:config') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysConfigService.delete(id);
        return Result.success();
    }

    @Operation(summary = "配置详情")
    @PreAuthorize("@ss.hasPermi('system:config') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<SysConfigVO> detail(@PathVariable Long id) {
        return Result.success(sysConfigService.getDetail(id));
    }
}
