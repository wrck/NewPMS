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
        return entity.getId();
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
