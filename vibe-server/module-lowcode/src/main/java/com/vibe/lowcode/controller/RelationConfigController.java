package com.vibe.lowcode.controller;

import com.vibe.common.base.PageQuery;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.lowcode.dto.LowcodeInstantiateDTO;
import com.vibe.lowcode.dto.LowcodeRelationConfigDTO;
import com.vibe.lowcode.service.LowcodeRelationConfigService;
import com.vibe.lowcode.vo.LowcodeRelationConfigVO;
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
 * 低代码关联页配置 Controller
 *
 * <p>路径：{@code /api/v1/lowcode/relations}</p>
 *
 * @author vibe
 */
@Slf4j
@Tag(name = "低代码关联页配置", description = "关联页 Schema 配置：CRUD / 复制 / 导入导出 / 模板实例化")
@RestController
@RequestMapping("/api/v1/lowcode/relations")
@RequiredArgsConstructor
public class RelationConfigController {

    private final LowcodeRelationConfigService relationConfigService;

    @Operation(summary = "分页查询关联页配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:list') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<LowcodeRelationConfigVO>> page(@ParameterObject PageQuery query,
                                                            @RequestParam(required = false) String keyword) {
        return Result.success(relationConfigService.page(query, keyword));
    }

    @Operation(summary = "关联页配置详情")
    @PreAuthorize("@ss.hasPermi('lowcode:config:list') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}")
    public Result<LowcodeRelationConfigVO> detail(@PathVariable Long id) {
        return Result.success(relationConfigService.getById(id));
    }

    @Operation(summary = "创建关联页配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody LowcodeRelationConfigDTO dto) {
        return Result.success(relationConfigService.create(dto));
    }

    @Operation(summary = "更新关联页配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LowcodeRelationConfigDTO dto) {
        relationConfigService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除关联页配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:remove') or hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        relationConfigService.delete(id);
        return Result.success();
    }

    @Operation(summary = "复制关联页配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/copy")
    public Result<Long> copy(@PathVariable Long id) {
        return Result.success(relationConfigService.copy(id));
    }

    @Operation(summary = "导出关联页 JSON Schema（下载文件）")
    @PreAuthorize("@ss.hasPermi('lowcode:config:list') or hasRole('SUPER_ADMIN')")
    @GetMapping("/{id}/export")
    public void exportJson(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String json = relationConfigService.exportJson(id);
        response.setContentType("application/json;charset=UTF-8");
        String filename = URLEncoder.encode("relation_config_" + id + ".json", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + filename);
        response.getWriter().write(json);
    }

    @Operation(summary = "导入关联页 JSON Schema")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/import")
    public Result<Long> importJson(@Valid @RequestBody LowcodeRelationConfigDTO dto) {
        return Result.success(relationConfigService.importJson(dto));
    }

    @Operation(summary = "基于模板实例化关联页配置")
    @PreAuthorize("@ss.hasPermi('lowcode:config:add') or hasRole('SUPER_ADMIN')")
    @PostMapping("/templates/{templateId}/instantiate")
    public Result<Long> instantiateFromTemplate(@PathVariable Long templateId,
                                                @Valid @RequestBody LowcodeInstantiateDTO dto) {
        return Result.success(relationConfigService.instantiateFromTemplate(templateId, dto.getConfigName()));
    }
}
