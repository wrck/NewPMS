package com.vibe.system.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysDictDataDTO;
import com.vibe.system.dto.SysDictDataQueryDTO;
import com.vibe.system.service.SysDictDataService;
import com.vibe.system.vo.SysDictDataVO;
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
 * 字典数据 Controller
 *
 * @author vibe
 */
@Tag(name = "字典数据", description = "字典数据 CRUD、按类型查询（带缓存）")
@RestController
@RequestMapping("/api/v1/dicts/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final SysDictDataService sysDictDataService;

    @Operation(summary = "分页查询字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<SysDictDataVO>> page(@ParameterObject SysDictDataQueryDTO query) {
        return Result.success(sysDictDataService.page(query));
    }

    @Operation(summary = "按 dictType 查询启用的字典数据（带缓存，公开）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-type/{dictType}")
    public Result<List<SysDictDataVO>> listByType(@PathVariable String dictType) {
        return Result.success(sysDictDataService.listByDictType(dictType));
    }

    @Operation(summary = "新增字典数据")
    @OperationLog(module = "字典管理", type = "INSERT", description = "新增字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysDictDataDTO dto) {
        return Result.success(sysDictDataService.create(dto));
    }

    @Operation(summary = "编辑字典数据")
    @OperationLog(module = "字典管理", type = "UPDATE", description = "编辑字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysDictDataDTO dto) {
        dto.setId(id);
        sysDictDataService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除字典数据")
    @OperationLog(module = "字典管理", type = "DELETE", description = "删除字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictDataService.delete(id);
        return Result.success();
    }

    @Operation(summary = "字典数据详情")
    @PreAuthorize("@ss.hasPermi('system:dict') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<SysDictDataVO> detail(@PathVariable Long id) {
        return Result.success(sysDictDataService.getDetail(id));
    }
}
