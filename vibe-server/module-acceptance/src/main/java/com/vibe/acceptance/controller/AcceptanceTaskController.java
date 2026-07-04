package com.vibe.acceptance.controller;

import com.vibe.acceptance.dto.AcceptanceTaskActionDTO;
import com.vibe.acceptance.dto.AcceptanceTaskCreateDTO;
import com.vibe.acceptance.dto.AcceptanceTaskQueryDTO;
import com.vibe.acceptance.service.AcceptanceTaskService;
import com.vibe.acceptance.vo.AcceptanceTaskVO;
import com.vibe.acceptance.vo.AcceptanceTestRecordVO;
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
 * 验收任务 Controller（验收流程：申请→内部审核→客户签核→完成）
 *
 * <p>路径：{@code /api/v1/acceptance/tasks}</p>
 *
 * @author vibe
 */
@Tag(name = "验收任务管理", description = "验收申请/内部审核/客户签核/测试记录")
@RestController
@RequestMapping("/api/v1/acceptance/tasks")
@RequiredArgsConstructor
public class AcceptanceTaskController {

    private final AcceptanceTaskService acceptanceTaskService;

    @Operation(summary = "分页查询验收任务")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<AcceptanceTaskVO>> page(@ParameterObject AcceptanceTaskQueryDTO query) {
        return Result.success(acceptanceTaskService.page(query));
    }

    @Operation(summary = "验收任务详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<AcceptanceTaskVO> detail(@PathVariable Long id) {
        return Result.success(acceptanceTaskService.getDetail(id));
    }

    @Operation(summary = "创建验收任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AcceptanceTaskCreateDTO dto) {
        return Result.success(acceptanceTaskService.create(dto));
    }

    @Operation(summary = "更新验收任务（仅草稿可改）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AcceptanceTaskCreateDTO dto) {
        acceptanceTaskService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除验收任务（仅草稿可删）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        acceptanceTaskService.delete(id);
        return Result.success();
    }

    @Operation(summary = "PM 提交验收申请")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody AcceptanceTaskActionDTO dto) {
        acceptanceTaskService.apply(dto);
        return Result.success();
    }

    @Operation(summary = "内部技术审核")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR')")
    @PostMapping("/internal-audit")
    public Result<Void> internalAudit(@Valid @RequestBody AcceptanceTaskActionDTO dto) {
        acceptanceTaskService.internalAudit(dto);
        return Result.success();
    }

    @Operation(summary = "发起客户签核")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/start-customer-sign")
    public Result<Void> startCustomerSign(@PathVariable Long id) {
        acceptanceTaskService.startCustomerSign(id);
        return Result.success();
    }

    @Operation(summary = "客户签核结果")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/customer-sign")
    public Result<Void> customerSign(@Valid @RequestBody AcceptanceTaskActionDTO dto) {
        acceptanceTaskService.customerSign(dto);
        return Result.success();
    }

    @Operation(summary = "查询验收任务的测试记录")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/test-records")
    public Result<List<AcceptanceTestRecordVO>> listTestRecords(@PathVariable Long id) {
        return Result.success(acceptanceTaskService.listTestRecords(id));
    }
}
