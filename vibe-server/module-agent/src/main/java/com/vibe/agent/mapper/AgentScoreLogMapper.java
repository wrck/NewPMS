package com.vibe.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.agent.entity.AgentScoreLogEntity;
import com.vibe.agent.vo.AgentScoreLogVO;
import com.vibe.annotation.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 代理商评分记录 Mapper
 *
 * <p>数据权限说明：</p>
 * <ul>
 *   <li>AGENT_ADMIN：通过 {@code @DataPermission(agentField = "agent_company_id")}
 *       限制仅能查看本公司的评分历史</li>
 *   <li>PM/SUPER_ADMIN/DIRECTOR：不追加条件</li>
 * </ul>
 *
 * @author vibe
 */
@Mapper
public interface AgentScoreLogMapper extends BaseMapper<AgentScoreLogEntity> {

    /**
     * 按代理商公司ID查询评分历史（按时间倒序）。
     *
     * <p>数据权限：AGENT_ADMIN 仅看本公司评分历史。</p>
     *
     * @param companyId 代理商公司ID
     */
    @DataPermission(table = "agent_score_log", agentField = "agent_company_id")
    List<AgentScoreLogVO> selectScoreHistory(@Param("companyId") Long companyId);

    /**
     * 按代理商公司ID聚合综合评分（加权平均）。
     *
     * <p>权重：及时性 30% + 质量 30% + 沟通 20% + 问题处理 20%。
     * 仅统计非 null 维度评分，按权重归一化计算。</p>
     *
     * @param companyId 代理商公司ID
     * @return 综合评分（0-100），无评分记录返回 null
     */
    BigDecimal selectWeightedAvgScore(@Param("companyId") Long companyId);

    /**
     * 按转包任务ID查询评分记录。
     */
    AgentScoreLogVO selectByTaskId(@Param("taskId") Long taskId);
}
