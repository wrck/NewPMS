package com.vibe.agent.service;

import com.vibe.agent.dto.AgentScoreDTO;
import com.vibe.agent.vo.AgentRankingVO;
import com.vibe.agent.vo.AgentScoreLogVO;

import java.util.List;

/**
 * 代理商评分服务
 *
 * <p>提供 PM 多维度打分（及时性/质量/沟通/问题处理）、综合评分计算（加权平均）、
 * 评分历史查询、代理商排名（影响分配优先级）。</p>
 *
 * @author vibe
 */
public interface AgentScoreService {

    /**
     * PM 对代理商打分。
     *
     * <p>评分维度：及时性（30%）/ 质量（30%）/ 沟通（20%）/ 问题处理（20%）。
     * 打分后自动计算加权平均综合评分，更新 agent_company.overall_score。</p>
     *
     * @return 评分记录ID
     */
    Long score(AgentScoreDTO dto);

    /**
     * 查询代理商评分历史。
     *
     * <p>数据权限：AGENT_ADMIN 仅看本公司评分历史。</p>
     */
    List<AgentScoreLogVO> getScoreHistory(Long companyId);

    /**
     * 查询某转包任务的评分记录。
     */
    AgentScoreLogVO getScoreByTaskId(Long taskId);

    /**
     * 代理商排名（按综合评分降序）。
     *
     * @param limit 前N名
     */
    List<AgentRankingVO> ranking(int limit);
}
