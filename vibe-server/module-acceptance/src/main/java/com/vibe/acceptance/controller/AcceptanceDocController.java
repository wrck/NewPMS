package com.vibe.acceptance.controller;

import com.vibe.acceptance.dto.AcceptanceDocQueryDTO;
import com.vibe.acceptance.dto.AcceptanceDocSaveDTO;
import com.vibe.acceptance.service.AcceptanceDocService;
import com.vibe.acceptance.vo.AcceptanceDocVO;
import com.vibe.annotation.OperationLog;
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

/**
 * 竣工文档 Controller
 *
 * <p>路径：{@code /api/v1/acceptance/docs}</p>
 *
 * @author vibe
 */
@Tag(name = "竣工文档管理", description = "As-Built 拓扑/设备清单/配置备份/测试报告/维护手册")
@RestController
@RequestMapping("/api/v1/acceptance/docs")
@RequiredArgsConstructor
public class AcceptanceDocController {

    private final AcceptanceDocService acceptanceDocService;

    @Operation(summary = "分页查询竣工文档")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<AcceptanceDocVO>> page(@ParameterObject AcceptanceDocQueryDTO query) {
        return Result.success(acceptanceDocService.page(query));
    }

    @Operation(summary = "竣工文档详情")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Result<AcceptanceDocVO> detail(@PathVariable Long id) {
        return Result.success(acceptanceDocService.getDetail(id));
    }

    @Operation(summary = "上传/创建竣工文档")
    @OperationLog(module = "竣工文档", type = "INSERT", description = "上传/创建竣工文档")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','ENGINEER')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AcceptanceDocSaveDTO dto) {
        return Result.success(acceptanceDocService.save(dto));
    }

    @Operation(summary = "更新竣工文档")
    @OperationLog(module = "竣工文档", type = "UPDATE", description = "更新竣工文档")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','ENGINEER')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AcceptanceDocSaveDTO dto) {
        acceptanceDocService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除竣工文档")
    @OperationLog(module = "竣工文档", type = "DELETE", description = "删除竣工文档")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        acceptanceDocService.delete(id);
        return Result.success();
    }
}
