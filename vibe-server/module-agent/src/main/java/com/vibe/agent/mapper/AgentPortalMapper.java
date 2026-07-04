package com.vibe.agent.mapper;

import com.vibe.agent.vo.AgentMessageVO;
import com.vibe.agent.vo.OutsourceTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代理商门户 Mapper
 *
 * <p>提供代理商 H5 端工作台聚合查询与消息 CRUD。</p>
 *
 * <p>数据权限说明：</p>
 * <ul>
 *   <li>所有任务查询均通过 {@code agent_company_id = #{agentCompanyId}} 强制隔离</li>
 *   <li>消息查询通过 {@code agent_company_id = #{agentCompanyId}} 隔离</li>
 * </ul>
 *
 * <p>避免使用 {@code @DataPermission} 注解，直接传 agentCompanyId 参数（来自 UserContext.tenantId），
 * 因为工作台查询是聚合查询，不局限于单表。</p>
 *
 * @author vibe
 */
@Mapper
public interface AgentPortalMapper {

    /**
     * 统计代理商某状态的任务数。
     *
     * @param agentCompanyId 代理商公司ID
     * @param status        任务状态
     * @return 任务数
     */
    int countByStatus(@Param("agentCompanyId") Long agentCompanyId,
                      @Param("status") String status);

    /**
     * 查询代理商某状态的最近 N 个任务（含关联信息，已脱敏）。
     *
     * @param agentCompanyId 代理商公司ID
     * @param status        任务状态
     * @param limit         返回条数
     * @return 任务列表
     */
    List<OutsourceTaskVO> selectTopNByStatus(@Param("agentCompanyId") Long agentCompanyId,
                                              @Param("status") String status,
                                              @Param("limit") int limit);

    /* ============ 代理商消息 ============ */

    /**
     * 查询代理商的消息列表（未读优先，按创建时间倒序）。
     *
     * @param agentCompanyId 代理商公司ID
     * @return 消息列表（最多 200 条）
     */
    List<AgentMessageVO> selectMessages(@Param("agentCompanyId") Long agentCompanyId);

    /**
     * 统计代理商未读消息数。
     *
     * @param agentCompanyId 代理商公司ID
     * @return 未读消息数
     */
    int countUnreadMessages(@Param("agentCompanyId") Long agentCompanyId);

    /**
     * 标记单条消息已读（带归属校验）。
     *
     * @param messageId      消息ID
     * @param agentCompanyId 代理商公司ID
     * @return 影响行数
     */
    int markMessageRead(@Param("messageId") Long messageId,
                        @Param("agentCompanyId") Long agentCompanyId);

    /**
     * 标记所有未读消息为已读。
     *
     * @param agentCompanyId 代理商公司ID
     * @return 影响行数
     */
    int markAllMessagesRead(@Param("agentCompanyId") Long agentCompanyId);

    /**
     * 插入消息（供事件监听器或定时任务调用）。
     *
     * @param message 消息对象
     * @return 影响行数
     */
    int insertMessage(AgentMessageVO message);
}
