package com.vibe.system.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysNoticeTemplateDTO;
import com.vibe.system.dto.SysNoticeTemplateQueryDTO;
import com.vibe.system.service.SysNoticeTemplateService;
import com.vibe.system.vo.SysNoticeTemplateVO;
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
 * 通知模板 Controller
 *
 * @author vibe
 */
@Tag(name = "通知模板", description = "通知模板 CRUD")
@RestController
@RequestMapping("/api/v1/notice-templates")
@RequiredArgsConstructor
public class SysNoticeTemplateController {

    private final SysNoticeTemplateService sysNoticeTemplateService;

    @Operation(summary = "分页查询通知模板")
    @PreAuthorize("@ss.hasPermi('system:notice') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<SysNoticeTemplateVO>> page(@ParameterObject SysNoticeTemplateQueryDTO query) {
        return Result.success(sysNoticeTemplateService.page(query));
    }

    @Operation(summary = "新增通知模板")
    @OperationLog(module = "通知模板", type = "INSERT", description = "新增通知模板")
    @PreAuthorize("@ss.hasPermi('system:notice') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SysNoticeTemplateDTO dto) {
        return Result.success(sysNoticeTemplateService.create(dto));
    }

    @Operation(summary = "编辑通知模板")
    @OperationLog(module = "通知模板", type = "UPDATE", description = "编辑通知模板")
    @PreAuthorize("@ss.hasPermi('system:notice') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysNoticeTemplateDTO dto) {
        dto.setId(id);
        sysNoticeTemplateService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除通知模板")
    @OperationLog(module = "通知模板", type = "DELETE", description = "删除通知模板")
    @PreAuthorize("@ss.hasPermi('system:notice') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysNoticeTemplateService.delete(id);
        return Result.success();
    }

    @Operation(summary = "通知模板详情")
    @PreAuthorize("@ss.hasPermi('system:notice') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<SysNoticeTemplateVO> detail(@PathVariable Long id) {
        return Result.success(sysNoticeTemplateService.getDetail(id));
    }

    @Operation(summary = "按模板编码查询")
    @PreAuthorize("@ss.hasPermi('system:notice') or hasRole('SUPER_ADMIN')")
    @GetMapping("/by-code/{code}")
    public Result<SysNoticeTemplateVO> byCode(@PathVariable String code) {
        return Result.success(sysNoticeTemplateService.getDetail(
                sysNoticeTemplateService.getByTemplateCode(code) == null
                        ? null : sysNoticeTemplateService.getByTemplateCode(code).getId()));
    }
}
