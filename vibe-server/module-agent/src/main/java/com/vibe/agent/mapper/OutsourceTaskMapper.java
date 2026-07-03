package com.vibe.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.agent.dto.OutsourceTaskQueryDTO;
import com.vibe.agent.entity.OutsourceTaskEntity;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.annotation.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 转包任务 Mapper
 *
 * <p>数据权限说明（核心）：</p>
 * <ul>
 *   <li>AGENT_ADMIN：通过 {@code @DataPermission(agentField = "agent_company_id")}
 *       限制仅能查看本公司任务（outsource_task.agent_company_id = tenantId）</li>
 *   <li>AGENT_ENGINEER：通过 {@code @DataPermission(agentEngineerField = "agent_engineer_id")}
 *       限制仅能看分配给自己的任务</li>
 *   <li>PM/SUPER_ADMIN/DIRECTOR：不追加条件，可看全部任务</li>
 * </ul>
 *
 * @author vibe
 */
@Mapper
public interface OutsourceTaskMapper extends BaseMapper<OutsourceTaskEntity> {

    /**
     * 分页查询转包任务（含项目名/任务名/代理商名关联）。
     *
     * <p>JOIN project / project_task / agent_company / agent_engineer，
     * 数据权限注解使用别名 {@code t}（指向 outsource_task 表）避免字段歧义。</p>
     *
     * @param page  分页参数
     * @param query 查询条件（projectId / agentCompanyId / status / keyword）
     */
    @DataPermission(table = "t", agentField = "agent_company_id", agentEngineerField = "agent_engineer_id")
    IPage<OutsourceTaskVO> selectTaskPage(IPage<OutsourceTaskVO> page,
                                          @Param("query") OutsourceTaskQueryDTO query);

    /**
     * 按 ID 查询转包任务详情（含关联信息）。
     */
    OutsourceTaskVO selectVoById(@Param("id") Long id);

    /**
     * 按项目任务ID查询转包任务（用于项目任务关联查询）。
     */
    OutsourceTaskVO selectByProjectTaskId(@Param("projectId") Long projectId,
                                          @Param("taskId") Long taskId);

    /**
     * 批量将超期任务标记为 OVERDUE（定时任务调用）。
     *
     * <p>仅更新 deadline &lt; 今天 且状态为非终态（PENDING/ACCEPTED/IN_PROGRESS/SUBMITTED/RETURNED）的任务。
     * 使用 @Update 注解直接执行 SQL，绕过数据权限拦截器（定时任务无用户上下文）。</p>
     *
     * @param today 当前日期
     * @return 受影响行数
     */
    @Update("UPDATE outsource_task SET status = 'OVERDUE', update_time = NOW() " +
            "WHERE deleted = 0 AND deadline IS NOT NULL AND deadline < #{today} " +
            "AND status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS', 'SUBMITTED', 'RETURNED')")
    int markOverdueTasks(@Param("today") LocalDate today);

    /**
     * 按代理商公司ID统计任务数（各状态）。
     *
     * @param agentCompanyId 代理商公司ID
     * @return 任务列表（用于统计）
     */
    List<OutsourceTaskVO> selectByAgentCompanyId(@Param("agentCompanyId") Long agentCompanyId);
}
