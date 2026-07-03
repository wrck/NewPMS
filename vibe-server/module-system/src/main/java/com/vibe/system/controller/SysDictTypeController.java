package com.vibe.system.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysDictTypeDTO;
import com.vibe.system.dto.SysDictTypeQueryDTO;
import com.vibe.system.service.SysDictTypeService;
import com.vibe.system.vo.SysDictTypeVO;
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
 * 字典类型 Controller
 *
 * @author vibe
 */
@Tag(name = "字典类型", description = "字典类型 CRUD")
@RestController
@RequestMapping("/api/v1/dicts/types")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final SysDictTypeService sysDictTypeService;

    @Operation(summary = "分页查询字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<SysDictTypeVO>> page(@ParameterObject SysDictTypeQueryDTO query) {
        return Result.success(sysDictTypeService.page(query));
    }

    @Operation(summary = "新增字典类型")
    @OperationLog(module = "字典管理", type = "INSERT", description = "新增字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysDictTypeDTO dto) {
        return Result.success(sysDictTypeService.create(dto));
    }

    @Operation(summary = "编辑字典类型")
    @OperationLog(module = "字典管理", type = "UPDATE", description = "编辑字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysDictTypeDTO dto) {
        dto.setId(id);
        sysDictTypeService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除字典类型")
    @OperationLog(module = "字典管理", type = "DELETE", description = "删除字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictTypeService.delete(id);
        return Result.success();
    }

    @Operation(summary = "字典类型详情")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<SysDictTypeVO> detail(@PathVariable Long id) {
        return Result.success(sysDictTypeService.getDetail(id));
    }
}
