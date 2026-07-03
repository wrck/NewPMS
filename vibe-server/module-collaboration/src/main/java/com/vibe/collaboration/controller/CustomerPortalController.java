package com.vibe.collaboration.controller;

import com.vibe.collaboration.service.CustomerPortalService;
import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.DocumentVO;
import com.vibe.collaboration.vo.ProjectProgressVO;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户门户 Controller
 *
 * <p>面向 CUSTOMER 角色的 H5 端接口，提供项目进度查看与文档下载能力。
 * 所有端点强制 {@code @PreAuthorize("hasRole('CUSTOMER')")}，并由 Service 层做项目归属校验。</p>
 *
 * @author vibe
 */
@Tag(name = "客户门户", description = "客户 H5 端项目进度查看与文档下载")
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPortalController {

    private final CustomerPortalService customerPortalService;

    @Operation(summary = "我的项目列表", description = "查询当前登录客户关联的项目列表（脱敏）")
    @GetMapping("/projects")
    public Result<List<CustomerProjectVO>> myProjects() {
        return Result.success(customerPortalService.getMyProjects());
    }

    @Operation(summary = "项目进度详情", description = "查询项目整体进度（含阶段时间线），仅限本人项目")
    @GetMapping("/projects/{projectId}/progress")
    public Result<ProjectProgressVO> projectProgress(
            @Parameter(name = "projectId", description = "项目ID", required = true, in = ParameterIn.PATH)
            @PathVariable Long projectId) {
        return Result.success(customerPortalService.getProjectProgress(projectId));
    }

    @Operation(summary = "项目文档列表", description = "查询项目可下载文档（含 MinIO 预签名 URL），仅限本人项目")
    @GetMapping("/projects/{projectId}/documents")
    public Result<List<DocumentVO>> projectDocuments(
            @Parameter(name = "projectId", description = "项目ID", required = true, in = ParameterIn.PATH)
            @PathVariable Long projectId) {
        return Result.success(customerPortalService.getProjectDocuments(projectId));
    }
}
