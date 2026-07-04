package com.vibe.integration.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.integration.dto.IntegrationCallLogQueryDTO;
import com.vibe.integration.service.IntegrationCallLogService;
import com.vibe.integration.vo.IntegrationCallLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 集成调用日志 Controller
 *
 * <p>路径：{@code /api/v1/integration/call-logs}</p>
 *
 * @author vibe
 */
@Tag(name = "集成调用日志", description = "外部系统调用历史与审计")
@RestController
@RequestMapping("/api/v1/integration/call-logs")
@RequiredArgsConstructor
public class IntegrationCallLogController {

    private final IntegrationCallLogService integrationCallLogService;

    @Operation(summary = "分页查询调用日志")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<IntegrationCallLogVO>> page(@ParameterObject IntegrationCallLogQueryDTO query) {
        return Result.success(integrationCallLogService.page(query));
    }

    @Operation(summary = "调用日志详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<IntegrationCallLogVO> detail(@PathVariable Long id) {
        return Result.success(integrationCallLogService.getDetail(id));
    }

    @Operation(summary = "删除调用日志")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        integrationCallLogService.delete(id);
        return Result.success();
    }

    @Operation(summary = "清空所有调用日志")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @PostMapping("/clear")
    public Result<Void> clearAll() {
        integrationCallLogService.clearAll();
        return Result.success();
    }
}
