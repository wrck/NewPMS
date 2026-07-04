package com.vibe.acceptance.controller;

import com.vibe.acceptance.dto.AcceptanceIssueQueryDTO;
import com.vibe.acceptance.dto.AcceptanceIssueSaveDTO;
import com.vibe.acceptance.service.AcceptanceIssueService;
import com.vibe.acceptance.vo.AcceptanceIssueVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验收遗留问题 Controller
 *
 * <p>路径：{@code /api/v1/acceptance/issues}</p>
 *
 * @author vibe
 */
@Tag(name = "验收遗留问题", description = "遗留问题登记/指派/整改/闭环")
@RestController
@RequestMapping("/api/v1/acceptance/issues")
@RequiredArgsConstructor
public class AcceptanceIssueController {

    private final AcceptanceIssueService acceptanceIssueService;

    @Operation(summary = "分页查询遗留问题")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<AcceptanceIssueVO>> page(@ParameterObject AcceptanceIssueQueryDTO query) {
        return Result.success(acceptanceIssueService.page(query));
    }

    @Operation(summary = "遗留问题详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<AcceptanceIssueVO> detail(@PathVariable Long id) {
        return Result.success(acceptanceIssueService.getDetail(id));
    }

    @Operation(summary = "创建遗留问题")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AcceptanceIssueSaveDTO dto) {
        return Result.success(acceptanceIssueService.save(dto));
    }

    @Operation(summary = "更新遗留问题")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AcceptanceIssueSaveDTO dto) {
        acceptanceIssueService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除遗留问题")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        acceptanceIssueService.delete(id);
        return Result.success();
    }

    @Operation(summary = "指派整改责任人")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestParam Long assigneeId) {
        acceptanceIssueService.assign(id, assigneeId);
        return Result.success();
    }

    @Operation(summary = "标记整改完成")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','ENGINEER')")
    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id) {
        acceptanceIssueService.resolve(id);
        return Result.success();
    }

    @Operation(summary = "闭环确认")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        acceptanceIssueService.close(id);
        return Result.success();
    }
}
