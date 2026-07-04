package com.vibe.agent.service.impl;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.mapper.AgentPortalMapper;
import com.vibe.agent.service.AgentPortalService;
import com.vibe.agent.vo.AgentMessageVO;
import com.vibe.agent.vo.AgentWorkbenchVO;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 代理商门户服务实现
 *
 * <p>核心职责：</p>
 * <ol>
 *   <li>数据隔离：通过 {@link UserContextHolder#getTenantId()} 获取代理商公司ID</li>
 *   <li>VO 脱敏：调用 {@link OutsourceTaskVO#desensitizeForAgent()} 屏蔽客户/合同/成本字段</li>
 *   <li>聚合查询：工作台统计 + 三类任务 top N</li>
 * </ol>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPortalServiceImpl implements AgentPortalService {

    /** 工作台每类任务返回的最大条数 */
    private static final int WORKBENCH_TOP_N = 5;

    private final AgentPortalMapper agentPortalMapper;

    @Override
    public AgentWorkbenchVO getWorkbench() {
        Long agentCompanyId = requireCurrentAgentCompanyId();

        AgentWorkbenchVO workbench = new AgentWorkbenchVO();

        // 统计卡片
        AgentWorkbenchVO.Summary summary = new AgentWorkbenchVO.Summary();
        summary.setPendingCount(agentPortalMapper.countByStatus(agentCompanyId, AgentConstant.TASK_PENDING));
        summary.setInProgressCount(agentPortalMapper.countByStatus(agentCompanyId, AgentConstant.TASK_IN_PROGRESS));
        summary.setSubmittedCount(agentPortalMapper.countByStatus(agentCompanyId, AgentConstant.TASK_SUBMITTED));
        summary.setOverdueCount(agentPortalMapper.countByStatus(agentCompanyId, AgentConstant.TASK_OVERDUE));
        summary.setUnreadMessageCount(agentPortalMapper.countUnreadMessages(agentCompanyId));
        workbench.setSummary(summary);

        // 三类任务 top N（已脱敏）
        workbench.setPendingTasks(desensitizeList(
                agentPortalMapper.selectTopNByStatus(agentCompanyId, AgentConstant.TASK_PENDING, WORKBENCH_TOP_N)));
        workbench.setInProgressTasks(desensitizeList(
                agentPortalMapper.selectTopNByStatus(agentCompanyId, AgentConstant.TASK_IN_PROGRESS, WORKBENCH_TOP_N)));
        workbench.setSubmittedTasks(desensitizeList(
                agentPortalMapper.selectTopNByStatus(agentCompanyId, AgentConstant.TASK_SUBMITTED, WORKBENCH_TOP_N)));

        return workbench;
    }

    /* ============ 消息通知 ============ */

    @Override
    public List<AgentMessageVO> getMyMessages() {
        Long agentCompanyId = requireCurrentAgentCompanyId();
        List<AgentMessageVO> msgs = agentPortalMapper.selectMessages(agentCompanyId);
        return msgs == null ? Collections.emptyList() : msgs;
    }

    @Override
    public int countUnreadMessages() {
        Long agentCompanyId = requireCurrentAgentCompanyId();
        return agentPortalMapper.countUnreadMessages(agentCompanyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessageRead(Long messageId) {
        if (messageId == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING);
        }
        Long agentCompanyId = requireCurrentAgentCompanyId();
        agentPortalMapper.markMessageRead(messageId, agentCompanyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllMessagesRead() {
        Long agentCompanyId = requireCurrentAgentCompanyId();
        agentPortalMapper.markAllMessagesRead(agentCompanyId);
    }

    /* ============ 私有辅助方法 ============ */

    /**
     * 获取当前登录代理商的公司ID（即 tenantId）。
     *
     * <p>仅 AGENT_ADMIN / AGENT_ENGINEER 角色可调用，其他角色返回 403。</p>
     */
    private Long requireCurrentAgentCompanyId() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getTenantId() == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }
        // 校验角色：必须是代理商角色
        List<String> roles = ctx.getRoles();
        if (roles == null || roles.isEmpty()
                || (!roles.contains(AgentConstant.ROLE_AGENT_ADMIN)
                    && !roles.contains(AgentConstant.ROLE_AGENT_ENGINEER)
                    && !roles.contains(AgentConstant.ROLE_SUPER_ADMIN))) {
            throw BusinessException.of(ResultCode.FORBIDDEN);
        }
        return ctx.getTenantId();
    }

    /**
     * 对代理商任务列表进行脱敏处理。
     */
    private List<OutsourceTaskVO> desensitizeList(List<OutsourceTaskVO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        list.forEach(OutsourceTaskVO::desensitizeForAgent);
        return list;
    }
}
