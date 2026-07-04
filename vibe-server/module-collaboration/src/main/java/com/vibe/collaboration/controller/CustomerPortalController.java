package com.vibe.collaboration.controller;

import com.vibe.collaboration.dto.CustomerAcceptanceSignDTO;
import com.vibe.collaboration.dto.CustomerCutoverApprovalDTO;
import com.vibe.collaboration.service.CustomerPortalService;
import com.vibe.collaboration.vo.CustomerAcceptanceTaskVO;
import com.vibe.collaboration.vo.CustomerCutoverPlanVO;
import com.vibe.collaboration.vo.CustomerMessageVO;
import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.CustomerTodoVO;
import com.vibe.collaboration.vo.DocumentVO;
import com.vibe.collaboration.vo.ProjectProgressVO;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户门户 Controller
 *
 * <p>面向 CUSTOMER 角色的 H5 端接口，提供项目进度查看/文档下载/割接审批/验收签核/消息通知能力。
 * 进度查看与文档下载强制 {@code @PreAuthorize("hasRole('CUSTOMER')")}，并由 Service 层做项目归属校验。
 * 割接审批/验收签核通过 token 访问，无需登录态，但提交结果时需要登录态做归属校验。</p>
 *
 * @author vibe
 */
@Tag(name = "客户门户", description = "客户 H5 端项目进度/文档/审批/签核/消息")
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerPortalController {

    private final CustomerPortalService customerPortalService;

    /* ============ 3.1 进度查看 ============ */

    @Operation(summary = "我的项目列表", description = "查询当前登录客户关联的项目列表（脱敏）")
    @GetMapping("/projects")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<List<CustomerProjectVO>> myProjects() {
        return Result.success(customerPortalService.getMyProjects());
    }

    @Operation(summary = "项目进度详情", description = "查询项目整体进度（含阶段时间线），仅限本人项目")
    @GetMapping("/projects/{projectId}/progress")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<ProjectProgressVO> projectProgress(
            @Parameter(name = "projectId", description = "项目ID", required = true, in = ParameterIn.PATH)
            @PathVariable Long projectId) {
        return Result.success(customerPortalService.getProjectProgress(projectId));
    }

    @Operation(summary = "项目文档列表", description = "查询项目可下载文档（含 MinIO 预签名 URL），仅限本人项目")
    @GetMapping("/projects/{projectId}/documents")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<List<DocumentVO>> projectDocuments(
            @Parameter(name = "projectId", description = "项目ID", required = true, in = ParameterIn.PATH)
            @PathVariable Long projectId) {
        return Result.success(customerPortalService.getProjectDocuments(projectId));
    }

    /* ============ 3.2 割接审批 ============ */

    @Operation(summary = "通过 token 查看割接方案详情", description = "客户无需登录即可查看待审批的割接方案详情（含步骤）")
    @GetMapping("/cutover/{token}")
    public Result<CustomerCutoverPlanVO> cutoverPlanByToken(
            @Parameter(name = "token", description = "客户签核链接 token", required = true, in = ParameterIn.PATH)
            @PathVariable String token) {
        return Result.success(customerPortalService.getCutoverPlanByToken(token));
    }

    @Operation(summary = "客户提交割接审批结果", description = "客户在 H5 端提交审批结果（APPROVED/REJECTED），需要登录态")
    @PostMapping("/cutover/approval")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<Void> submitCutoverApproval(@RequestBody CustomerCutoverApprovalDTO dto) {
        customerPortalService.submitCutoverApproval(dto);
        return Result.success();
    }

    /* ============ 3.3 验收签核 ============ */

    @Operation(summary = "通过 token 查看验收任务详情", description = "客户无需登录即可查看待签核的验收任务详情（含测试记录）")
    @GetMapping("/acceptance/{token}")
    public Result<CustomerAcceptanceTaskVO> acceptanceTaskByToken(
            @Parameter(name = "token", description = "客户签核链接 token", required = true, in = ParameterIn.PATH)
            @PathVariable String token) {
        return Result.success(customerPortalService.getAcceptanceTaskByToken(token));
    }

    @Operation(summary = "客户提交验收签核结果", description = "客户在 H5 端提交签核结果（PASS/CONDITIONAL_PASS/REJECT），需要登录态")
    @PostMapping("/acceptance/sign")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<Void> submitAcceptanceSign(@RequestBody CustomerAcceptanceSignDTO dto) {
        customerPortalService.submitAcceptanceSign(dto);
        return Result.success();
    }

    /* ============ 3.4 待办列表 ============ */

    @Operation(summary = "我的待办", description = "聚合查询当前客户的所有待办（待审批割接方案 + 待签核验收任务）")
    @GetMapping("/todos")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<List<CustomerTodoVO>> myTodos() {
        return Result.success(customerPortalService.getMyTodos());
    }

    /* ============ 3.5 消息通知 ============ */

    @Operation(summary = "我的消息列表", description = "查询当前登录客户的消息列表（按时间倒序，未读优先）")
    @GetMapping("/messages")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<List<CustomerMessageVO>> myMessages() {
        return Result.success(customerPortalService.getMyMessages());
    }

    @Operation(summary = "未读消息数", description = "统计当前登录客户的未读消息数")
    @GetMapping("/messages/unread-count")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<Integer> unreadMessageCount() {
        return Result.success(customerPortalService.countUnreadMessages());
    }

    @Operation(summary = "标记消息已读", description = "将指定消息标记为已读")
    @PostMapping("/messages/{messageId}/read")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<Void> markMessageRead(
            @Parameter(name = "messageId", description = "消息ID", required = true, in = ParameterIn.PATH)
            @PathVariable Long messageId) {
        customerPortalService.markMessageRead(messageId);
        return Result.success();
    }

    @Operation(summary = "全部标记已读", description = "将当前客户的所有未读消息标记为已读")
    @PostMapping("/messages/read-all")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<Void> markAllMessagesRead() {
        customerPortalService.markAllMessagesRead();
        return Result.success();
    }
}
