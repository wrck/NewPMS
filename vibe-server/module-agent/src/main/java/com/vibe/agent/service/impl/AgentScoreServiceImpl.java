package com.vibe.agent.service.impl;

import com.vibe.agent.dto.AgentScoreDTO;
import com.vibe.agent.entity.AgentScoreLogEntity;
import com.vibe.agent.mapper.AgentScoreLogMapper;
import com.vibe.agent.service.AgentCompanyService;
import com.vibe.agent.service.AgentScoreService;
import com.vibe.agent.vo.AgentRankingVO;
import com.vibe.agent.vo.AgentScoreLogVO;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.event.DomainEventPublisher;
import com.vibe.event.events.AgentScoredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 代理商评分服务实现
 *
 * <p>评分维度：及时性（30%）/ 质量（30%）/ 沟通（20%）/ 问题处理（20%）。
 * 打分后自动计算加权平均综合评分，更新 agent_company.overall_score。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentScoreServiceImpl implements AgentScoreService {

    private final AgentScoreLogMapper scoreLogMapper;
    private final AgentCompanyService agentCompanyService;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long score(AgentScoreDTO dto) {
        // 校验代理商公司存在
        agentCompanyService.getDetail(dto.getAgentCompanyId());

        AgentScoreLogEntity entity = new AgentScoreLogEntity();
        entity.setAgentCompanyId(dto.getAgentCompanyId());
        entity.setOutsourceTaskId(dto.getOutsourceTaskId());
        entity.setScoreTimeliness(dto.getScoreTimeliness());
        entity.setScoreQuality(dto.getScoreQuality());
        entity.setScoreCommunication(dto.getScoreCommunication());
        entity.setScoreIssue(dto.getScoreIssue());
        entity.setScorerId(UserContextHolder.getUserId());
        entity.setRemark(dto.getRemark());
        scoreLogMapper.insert(entity);

        // 重新计算并更新代理商综合评分（加权平均）
        agentCompanyService.refreshOverallScore(dto.getAgentCompanyId());

        log.info("PM 对代理商打分: companyId={}, scoreLogId={}, scorerId={}",
                dto.getAgentCompanyId(), entity.getId(), entity.getScorerId());

        // 发布代理商评分领域事件
        // 综合得分 = 及时性*0.3 + 质量*0.3 + 沟通*0.2 + 问题处理*0.2
        double total = weightedScore(entity);
        Double qualityScore = entity.getScoreQuality() == null
                ? null : entity.getScoreQuality().doubleValue();
        Double scheduleScore = entity.getScoreTimeliness() == null
                ? null : entity.getScoreTimeliness().doubleValue();
        domainEventPublisher.publish(new AgentScoredEvent(
                entity.getId(), dto.getAgentCompanyId(), null,
                total, qualityScore, scheduleScore));
        return entity.getId();
    }

    /**
     * 计算加权综合得分（0-100）。
     */
    private double weightedScore(AgentScoreLogEntity e) {
        double t = e.getScoreTimeliness() == null ? 0 : e.getScoreTimeliness().doubleValue();
        double q = e.getScoreQuality() == null ? 0 : e.getScoreQuality().doubleValue();
        double c = e.getScoreCommunication() == null ? 0 : e.getScoreCommunication().doubleValue();
        double i = e.getScoreIssue() == null ? 0 : e.getScoreIssue().doubleValue();
        return t * 0.3 + q * 0.3 + c * 0.2 + i * 0.2;
    }

    @Override
    public List<AgentScoreLogVO> getScoreHistory(Long companyId) {
        if (companyId == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "代理商公司ID不能为空");
        }
        // @DataPermission 在 Mapper 方法上：AGENT_ADMIN 仅看本公司评分历史
        return scoreLogMapper.selectScoreHistory(companyId);
    }

    @Override
    public AgentScoreLogVO getScoreByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return scoreLogMapper.selectByTaskId(taskId);
    }

    @Override
    public List<AgentRankingVO> ranking(int limit) {
        return agentCompanyService.ranking(limit);
    }
}
