package com.vibe.agent.service;

import com.vibe.agent.vo.AgentMessageVO;
import com.vibe.agent.vo.AgentWorkbenchVO;

import java.util.List;

/**
 * 代理商门户服务（H5 端工作台 + 消息）
 *
 * <p>核心职责：</p>
 * <ol>
 *   <li>工作台聚合：统计卡片（待接单/进行中/待审核/超期）+ 三类 top N 任务</li>
 *   <li>消息通知：列表/未读数/标记已读</li>
 * </ol>
 *
 * <p>数据隔离：通过 {@code UserContextHolder.getTenantId()} 获取代理商公司ID。</p>
 *
 * @author vibe
 */
public interface AgentPortalService {

    /**
     * 获取代理商工作台首页数据。
     *
     * <p>返回内容：</p>
     * <ul>
     *   <li>统计卡片：待接单/进行中/待审核/已超期 + 未读消息数</li>
     *   <li>三类任务的 top 5 列表（待接单/进行中/待审核）</li>
     * </ul>
     */
    AgentWorkbenchVO getWorkbench();

    /* ============ 消息通知 ============ */

    /**
     * 查询当前代理商的消息列表。
     */
    List<AgentMessageVO> getMyMessages();

    /**
     * 统计当前代理商未读消息数。
     */
    int countUnreadMessages();

    /**
     * 标记单条消息已读。
     */
    void markMessageRead(Long messageId);

    /**
     * 标记所有未读消息为已读。
     */
    void markAllMessagesRead();
}
