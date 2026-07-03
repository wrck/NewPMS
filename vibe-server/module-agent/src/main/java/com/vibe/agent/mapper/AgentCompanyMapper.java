package com.vibe.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.agent.dto.AgentCompanyQueryDTO;
import com.vibe.agent.entity.AgentCompanyEntity;
import com.vibe.agent.vo.AgentCompanyVO;
import com.vibe.annotation.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 代理商公司 Mapper
 *
 * <p>数据权限说明：</p>
 * <ul>
 *   <li>AGENT_ADMIN：通过 {@code @DataPermission(agentField = "id")} 限制仅能查看本公司档案
 *       （agent_company 表的 id 字段对应 AGENT_ADMIN 的 tenantId）</li>
 *   <li>其他角色：不追加条件（PM/SUPER_ADMIN/DIRECTOR 可看全部代理商）</li>
 * </ul>
 *
 * @author vibe
 */
@Mapper
public interface AgentCompanyMapper extends BaseMapper<AgentCompanyEntity> {

    /**
     * 分页查询代理商公司列表（含合作区域/产品线/状态筛选）。
     *
     * <p>数据权限：AGENT_ADMIN 仅看本公司（id = tenantId）。</p>
     */
    @DataPermission(table = "agent_company", agentField = "id")
    IPage<AgentCompanyVO> selectCompanyPage(IPage<AgentCompanyVO> page,
                                            @Param("query") AgentCompanyQueryDTO query);

    /**
     * 按 ID 查询代理商详情（VO 形式）。
     */
    AgentCompanyVO selectVoById(@Param("id") Long id);

    /**
     * 按服务区域查询代理商（用于任务分配时筛选）。
     */
    List<AgentCompanyVO> selectByRegion(@Param("region") String region);

    /**
     * 代理商排名（按综合评分降序）。
     *
     * @param limit 返回前 N 名
     */
    @DataPermission(ignore = true)
    List<AgentCompanyVO> selectRanking(@Param("limit") int limit);

    /**
     * 按公司ID聚合综合评分（最近 N 次评分的加权平均）。
     *
     * @param companyId 代理商公司ID
     * @return 综合评分，无评分记录返回 null
     */
    BigDecimal selectAvgScoreByCompanyId(@Param("companyId") Long companyId);
}
