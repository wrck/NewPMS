package com.vibe.lowcode.controller;

import com.vibe.common.base.PageQuery;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.lowcode.dto.LowcodeInstantiateDTO;
import com.vibe.lowcode.dto.LowcodeTemplateDTO;
import com.vibe.lowcode.service.LowcodeTemplateService;
import com.vibe.lowcode.vo.LowcodeTemplateVO;
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
 * 低代码模板 Controller
 *
 * <p>路径：{@code /api/v1/lowcode/templates}</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "低代码模板管理", description = "模板 CRUD / 复制 / 导入导出 / 实例化校验")
@RestController
@RequestMapping("/api/v1/lowcode/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final LowcodeTemplateService templateService;

    @Operation(summary = "分页查询模板")
    @PreAuthorize("@ss.hasPermi('lowcode:template:list') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<LowcodeTemplateVO>> page(@ParameterObject PageQuery query,
                                                       @RequestParam(required = false) String keyword) {
        return Result.success(templateService.page(query, keyword));
    }

    @Operation(summary = "模板详情")
    @PreAuthorize("@ss.hasPermi('lowcode:template:list') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<LowcodeTemplateVO> detail(@PathVariable Long id) {
        return Result.success(templateService.getById(id));
    }

    @Operation(summary = "创建模板")
    @PreAuthorize("@ss.hasPermi('lowcode:template:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody LowcodeTemplateDTO dto) {
        return Result.success(templateService.create(dto));
    }

    @Operation(summary = "更新模板")
    @PreAuthorize("@ss.hasPermi('lowcode:template:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LowcodeTemplateDTO dto) {
        templateService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除模板")
    @PreAuthorize("@ss.hasPermi('lowcode:template:remove') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }

    @Operation(summary = "复制模板")
    @PreAuthorize("@ss.hasPermi('lowcode:template:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/copy")
    public Result<Long> copy(@PathVariable Long id) {
        return Result.success(templateService.copy(id));
    }

    @Operation(summary = "导出模板 JSON Schema（下载文件）")
    @PreAuthorize("@ss.hasPermi('lowcode:template:list') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}/export")
    public void exportJson(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String json = templateService.exportJson(id);
        response.setContentType("application/json;charset=UTF-8");
        String filename = URLEncoder.encode("template_" + id + ".json", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + filename);
        response.getWriter().write(json);
    }

    @Operation(summary = "导入模板 JSON Schema")
    @PreAuthorize("@ss.hasPermi('lowcode:template:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/import")
    public Result<Long> importJson(@Valid @RequestBody LowcodeTemplateDTO dto) {
        return Result.success(templateService.importJson(dto));
    }

    @Operation(summary = "校验模板可实例化（返回模板 ID）")
    @PreAuthorize("@ss.hasPermi('lowcode:template:list') or hasRole('SUPER_ADMIN')")
    @PostMapping("/{templateId}/instantiate")
    public Result<Long> instantiateFromTemplate(@PathVariable Long templateId,
                                                @Valid @RequestBody LowcodeInstantiateDTO dto) {
        return Result.success(templateService.instantiateFromTemplate(templateId, dto.getConfigName()));
    }
}
