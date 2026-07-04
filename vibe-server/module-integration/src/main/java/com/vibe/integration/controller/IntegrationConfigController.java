package com.vibe.integration.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.integration.dto.IntegrationConfigQueryDTO;
import com.vibe.integration.dto.IntegrationConfigSaveDTO;
import com.vibe.integration.service.IntegrationConfigService;
import com.vibe.integration.vo.IntegrationConfigVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 集成配置 Controller
 *
 * <p>路径：{@code /api/v1/integration/configs}</p>
 *
 * @author vibe
 */
@Tag(name = "集成配置", description = "外部系统连接信息管理")
@RestController
@RequestMapping("/api/v1/integration/configs")
@RequiredArgsConstructor
public class IntegrationConfigController {

    private final IntegrationConfigService integrationConfigService;

    @Operation(summary = "分页查询集成配置")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<IntegrationConfigVO>> page(@ParameterObject IntegrationConfigQueryDTO query) {
        return Result.success(integrationConfigService.page(query));
    }

    @Operation(summary = "集成配置详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<IntegrationConfigVO> detail(@PathVariable Long id) {
        return Result.success(integrationConfigService.getDetail(id));
    }

    @Operation(summary = "按系统编码查询")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-code/{systemCode}")
    public Result<IntegrationConfigVO> byCode(@PathVariable String systemCode) {
        return Result.success(integrationConfigService.getBySystemCode(systemCode));
    }

    @Operation(summary = "查询所有启用的集成配置")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/enabled")
    public Result<List<IntegrationConfigVO>> listEnabled() {
        return Result.success(integrationConfigService.listEnabled());
    }

    @Operation(summary = "新增集成配置")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody IntegrationConfigSaveDTO dto) {
        return Result.success(integrationConfigService.save(dto));
    }

    @Operation(summary = "更新集成配置")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody IntegrationConfigSaveDTO dto) {
        integrationConfigService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除集成配置")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        integrationConfigService.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用集成配置")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PutMapping("/{id}/enabled")
    public Result<Void> toggleEnabled(@PathVariable Long id,
                                      @RequestParam Integer enabled) {
        integrationConfigService.toggleEnabled(id, enabled);
        return Result.success();
    }

    @Operation(summary = "测试连接")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        return Result.success(integrationConfigService.testConnection(id));
    }
}
