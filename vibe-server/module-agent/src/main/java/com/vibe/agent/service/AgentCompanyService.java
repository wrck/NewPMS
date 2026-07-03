package com.vibe.agent.service;

import com.vibe.agent.dto.AgentCompanyDTO;
import com.vibe.agent.dto.AgentCompanyQueryDTO;
import com.vibe.agent.vo.AgentCompanyVO;
import com.vibe.agent.vo.AgentRankingVO;
import com.vibe.common.result.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 代理商公司服务
 *
 * <p>提供代理商企业档案 CRUD、合作区域/产品线/状态管理、综合评分维护、排名查询。</p>
 *
 * @author vibe
 */
public interface AgentCompanyService {

    /**
     * 分页查询代理商公司（数据权限：AGENT_ADMIN 仅看本公司）。
     */
    PageResult<AgentCompanyVO> page(AgentCompanyQueryDTO query);

    /**
     * 查询代理商详情。
     */
    AgentCompanyVO getDetail(Long id);

    /**
     * 新增代理商公司。
     *
     * @return 公司ID
     */
    Long create(AgentCompanyDTO dto);

    /**
     * 编辑代理商公司。
     */
    void update(AgentCompanyDTO dto);

    /**
     * 删除代理商公司（逻辑删除）。
     */
    void delete(Long id);

    /**
     * 变更合作状态（ACTIVE/SUSPENDED/TERMINATED）。
     */
    void changeStatus(Long id, String status);

    /**
     * 代理商排名（按综合评分降序）。
     *
     * @param limit 前N名
     */
    List<AgentRankingVO> ranking(int limit);

    /**
     * 更新代理商综合评分（评分提交后调用）。
     *
     * @param companyId 代理商公司ID
     */
    void refreshOverallScore(Long companyId);

    /**
     * 按区域查询可用代理商（用于任务分配推荐）。
     */
    List<AgentCompanyVO> listByRegion(String region);

    /**
     * 校验代理商公司是否存在且为活跃状态。
     */
    void validateActive(Long companyId);
}
