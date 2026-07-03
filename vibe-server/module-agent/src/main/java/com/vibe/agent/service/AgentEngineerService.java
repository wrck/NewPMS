package com.vibe.agent.service;

import com.vibe.agent.dto.AgentEngineerDTO;
import com.vibe.agent.dto.AgentEngineerQueryDTO;
import com.vibe.agent.vo.AgentEngineerVO;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.common.result.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 代理商工程师服务
 *
 * <p>提供工程师档案 CRUD、技能/认证管理、启用停用、项目历史查询、质量评分维护。</p>
 *
 * @author vibe
 */
public interface AgentEngineerService {

    /**
     * 分页查询工程师（数据权限：AGENT_ADMIN 仅看本公司工程师）。
     */
    PageResult<AgentEngineerVO> page(AgentEngineerQueryDTO query);

    /**
     * 按代理商公司ID查询工程师列表。
     */
    List<AgentEngineerVO> listByCompanyId(Long companyId);

    /**
     * 查询工程师详情。
     */
    AgentEngineerVO getDetail(Long id);

    /**
     * 新增工程师。
     */
    Long create(AgentEngineerDTO dto);

    /**
     * 编辑工程师。
     */
    void update(AgentEngineerDTO dto);

    /**
     * 删除工程师（逻辑删除）。
     */
    void delete(Long id);

    /**
     * 启用/停用工程师（ACTIVE/DISABLED）。
     */
    void changeStatus(Long id, String status);

    /**
     * 查询工程师项目历史（参与过的转包任务）。
     *
     * <p>数据权限：AGENT_ADMIN/AGENT_ENGINEER 仅看本公司/自己的任务。</p>
     */
    List<OutsourceTaskVO> getTaskHistory(Long engineerId);

    /**
     * 更新工程师质量评分。
     */
    void updateQualityScore(Long engineerId, BigDecimal score);

    /**
     * 校验工程师是否存在、属于指定公司、且为启用状态。
     */
    void validateActive(Long engineerId, Long expectedCompanyId);
}
