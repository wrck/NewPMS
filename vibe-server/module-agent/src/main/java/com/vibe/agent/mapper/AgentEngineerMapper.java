package com.vibe.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.agent.dto.AgentEngineerQueryDTO;
import com.vibe.agent.entity.AgentEngineerEntity;
import com.vibe.agent.vo.AgentEngineerVO;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.annotation.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代理商工程师 Mapper
 *
 * <p>数据权限说明：</p>
 * <ul>
 *   <li>AGENT_ADMIN：通过 {@code @DataPermission(agentField = "agent_company_id")}
 *       限制仅能查看本公司工程师（agent_engineer.agent_company_id = tenantId）</li>
 *   <li>AGENT_ENGINEER：默认按 agent_engineer_id 过滤，但工程师表无该字段，
 *       故在 Service 层显式按 userId 过滤</li>
 * </ul>
 *
 * @author vibe
 */
@Mapper
public interface AgentEngineerMapper extends BaseMapper<AgentEngineerEntity> {

    /**
     * 分页查询代理商工程师列表。
     *
     * <p>数据权限：AGENT_ADMIN 仅看本公司工程师。</p>
     */
    @DataPermission(table = "agent_engineer", agentField = "agent_company_id")
    IPage<AgentEngineerVO> selectEngineerPage(IPage<AgentEngineerVO> page,
                                              @Param("query") AgentEngineerQueryDTO query);

    /**
     * 按 ID 查询工程师详情。
     */
    AgentEngineerVO selectVoById(@Param("id") Long id);

    /**
     * 按代理商公司ID查询工程师列表（不分页）。
     */
    List<AgentEngineerVO> selectByCompanyId(@Param("companyId") Long companyId);

    /**
     * 查询工程师的项目历史（参与过的转包任务列表）。
     *
     * <p>数据权限：AGENT_ADMIN/AGENT_ENGINEER 仅看本公司/自己的任务。</p>
     *
     * @param engineerId 工程师ID
     */
    @DataPermission(table = "t", agentField = "agent_company_id", agentEngineerField = "agent_engineer_id")
    List<OutsourceTaskVO> selectEngineerTaskHistory(@Param("engineerId") Long engineerId);
}
