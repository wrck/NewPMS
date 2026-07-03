package com.vibe.project.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.project.dto.CustomerDTO;
import com.vibe.project.dto.CustomerQueryDTO;
import com.vibe.project.service.CustomerService;
import com.vibe.project.vo.CustomerVO;
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
 * 客户管理 Controller
 *
 * @author vibe
 */
@Tag(name = "客户管理", description = "客户 CRUD 与查询")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "分页查询客户")
    @PreAuthorize("@ss.hasPermi('project:customer') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<CustomerVO>> page(@ParameterObject CustomerQueryDTO query) {
        return Result.success(customerService.page(query));
    }

    @Operation(summary = "客户详情")
    @PreAuthorize("@ss.hasPermi('project:customer') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<CustomerVO> detail(@PathVariable Long id) {
        return Result.success(customerService.getDetail(id));
    }

    @Operation(summary = "新增客户")
    @OperationLog(module = "客户管理", type = "INSERT", description = "新增客户")
    @PreAuthorize("@ss.hasPermi('project:customer:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CustomerDTO dto) {
        return Result.success(customerService.create(dto));
    }

    @Operation(summary = "编辑客户")
    @OperationLog(module = "客户管理", type = "UPDATE", description = "编辑客户")
    @PreAuthorize("@ss.hasPermi('project:customer:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerDTO dto) {
        dto.setId(id);
        customerService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除客户")
    @OperationLog(module = "客户管理", type = "DELETE", description = "删除客户")
    @PreAuthorize("@ss.hasPermi('project:customer:add') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }
}
