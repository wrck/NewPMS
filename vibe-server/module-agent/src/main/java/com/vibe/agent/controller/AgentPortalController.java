package com.vibe.agent.controller;

import com.vibe.agent.service.AgentPortalService;
import com.vibe.agent.vo.AgentMessageVO;
import com.vibe.agent.vo.AgentWorkbenchVO;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 代理商门户 Controller（H5 端）
 *
 * <p>面向 AGENT_ADMIN / AGENT_ENGINEER 角色的 H5 工作台与消息接口。</p>
 *
 * <p>路径：{@code /api/v1/agent}</p>
 *
 * <p><b>说明：</b>转包任务的接单/拒绝/指派/提交交付物等操作仍由
 * {@link OutsourceTaskController} 与 {@link OutsourceDeliverableController} 提供，
 * 本 Controller 仅提供 H5 端工作台聚合查询与消息通知能力。</p>
 *
 * @author vibe
 */
@Tag(name = "代理商门户", description = "H5 工作台 + 消息通知")
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentPortalController {

    private final AgentPortalService agentPortalService;

    /* ============ 工作台 ============ */

    @Operation(summary = "代理商工作台", description = "聚合统计卡片（待接单/进行中/待审核/超期 + 未读消息数）与三类 top 5 任务列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping("/workbench")
    public Result<AgentWorkbenchVO> workbench() {
        return Result.success(agentPortalService.getWorkbench());
    }

    /* ============ 消息通知 ============ */

    @Operation(summary = "我的消息列表", description = "查询当前代理商的消息列表（未读优先，按时间倒序）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping("/messages")
    public Result<List<AgentMessageVO>> myMessages() {
        return Result.success(agentPortalService.getMyMessages());
    }

    @Operation(summary = "未读消息数", description = "统计当前代理商的未读消息数")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN','AGENT_ENGINEER')")
    @GetMapping("/messages/unread-count")
    public Result<Integer> unreadMessageCount() {
        return Result.success(agentPortalService.countUnreadMessages());
    }

    @Operation(summary = "标记消息已读", description = "将指定消息标记为已读（带归属校验）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN','AGENT_ENGINEER')")
    @PostMapping("/messages/{messageId}/read")
    public Result<Void> markMessageRead(
            @Parameter(name = "messageId", description = "消息ID", required = true, in = ParameterIn.PATH)
            @PathVariable Long messageId) {
        agentPortalService.markMessageRead(messageId);
        return Result.success();
    }

    @Operation(summary = "全部标记已读", description = "将当前代理商的所有未读消息标记为已读")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN','AGENT_ENGINEER')")
    @PostMapping("/messages/read-all")
    public Result<Void> markAllMessagesRead() {
        agentPortalService.markAllMessagesRead();
        return Result.success();
    }
}
