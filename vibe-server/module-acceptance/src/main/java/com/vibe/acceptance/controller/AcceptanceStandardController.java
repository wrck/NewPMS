package com.vibe.acceptance.controller;

import com.vibe.acceptance.dto.AcceptanceStandardQueryDTO;
import com.vibe.acceptance.dto.AcceptanceStandardSaveDTO;
import com.vibe.acceptance.service.AcceptanceStandardService;
import com.vibe.acceptance.vo.AcceptanceStandardVO;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
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
 * 验收标准 Controller
 *
 * <p>路径：{@code /api/v1/acceptance/standards}</p>
 *
 * @author vibe
 */
@Tag(name = "验收标准管理", description = "验收标准模板库与检查项定义")
@RestController
@RequestMapping("/api/v1/acceptance/standards")
@RequiredArgsConstructor
public class AcceptanceStandardController {

    private final AcceptanceStandardService acceptanceStandardService;

    @Operation(summary = "分页查询验收标准")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<AcceptanceStandardVO>> page(@ParameterObject AcceptanceStandardQueryDTO query) {
        return Result.success(acceptanceStandardService.page(query));
    }

    @Operation(summary = "查询全部启用的验收标准（下拉选择）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/enabled")
    public Result<List<AcceptanceStandardVO>> listEnabled() {
        return Result.success(acceptanceStandardService.listEnabled());
    }

    @Operation(summary = "验收标准详情（含检查项）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<AcceptanceStandardVO> detail(@PathVariable Long id) {
        return Result.success(acceptanceStandardService.getDetail(id));
    }

    @Operation(summary = "创建验收标准")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AcceptanceStandardSaveDTO dto) {
        return Result.success(acceptanceStandardService.save(dto));
    }

    @Operation(summary = "更新验收标准（含检查项全量替换）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AcceptanceStandardSaveDTO dto) {
        acceptanceStandardService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除验收标准")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        acceptanceStandardService.delete(id);
        return Result.success();
    }
}
