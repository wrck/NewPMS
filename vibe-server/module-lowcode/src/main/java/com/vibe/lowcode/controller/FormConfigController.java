package com.vibe.lowcode.controller;

import com.vibe.common.base.PageQuery;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.lowcode.dto.LowcodeFormConfigDTO;
import com.vibe.lowcode.dto.LowcodeInstantiateDTO;
import com.vibe.lowcode.service.LowcodeFormConfigService;
import com.vibe.lowcode.vo.LowcodeFormConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 低代码表单配置 Controller
 *
 * <p>路径：{@code /api/v1/lowcode/forms}</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "低代码表单配置", description = "表单 Schema 配置：CRUD / 复制 / 导入导出 / 模板实例化")
@RestController
@RequestMapping("/api/v1/lowcode/forms")
@RequiredArgsConstructor
public class FormConfigController {

    private final LowcodeFormConfigService formConfigService;

    @Operation(summary = "分页查询表单配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:list') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<LowcodeFormConfigVO>> page(@ParameterObject PageQuery query,
                                                         @RequestParam(required = false) String keyword) {
        return Result.success(formConfigService.page(query, keyword));
    }

    @Operation(summary = "表单配置详情")
    @PreAuthorize("@ss.hasPermi('lowcode:config:list') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<LowcodeFormConfigVO> detail(@PathVariable Long id) {
        return Result.success(formConfigService.getById(id));
    }

    @Operation(summary = "创建表单配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody LowcodeFormConfigDTO dto) {
        return Result.success(formConfigService.create(dto));
    }

    @Operation(summary = "更新表单配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LowcodeFormConfigDTO dto) {
        formConfigService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除表单配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:remove') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        formConfigService.delete(id);
        return Result.success();
    }

    @Operation(summary = "复制表单配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/copy")
    public Result<Long> copy(@PathVariable Long id) {
        return Result.success(formConfigService.copy(id));
    }

    @Operation(summary = "导出表单 JSON Schema（下载文件）")
    @PreAuthorize("@ss.hasPermi('lowcode:config:list') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}/export")
    public void exportJson(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String json = formConfigService.exportJson(id);
        response.setContentType("application/json;charset=UTF-8");
        String filename = URLEncoder.encode("form_config_" + id + ".json", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + filename);
        response.getWriter().write(json);
    }

    @Operation(summary = "导入表单 JSON Schema")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/import")
    public Result<Long> importJson(@Valid @RequestBody LowcodeFormConfigDTO dto) {
        return Result.success(formConfigService.importJson(dto));
    }

    @Operation(summary = "基于模板实例化表单配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/templates/{templateId}/instantiate")
    public Result<Long> instantiateFromTemplate(@PathVariable Long templateId,
                                                @Valid @RequestBody LowcodeInstantiateDTO dto) {
        return Result.success(formConfigService.instantiateFromTemplate(templateId, dto.getConfigName()));
    }
}
