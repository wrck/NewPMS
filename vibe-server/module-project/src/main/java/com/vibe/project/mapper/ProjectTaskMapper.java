package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.annotation.DataPermission;
import com.vibe.project.dto.ProjectTaskQueryDTO;
import com.vibe.project.entity.ProjectTaskEntity;
import com.vibe.project.vo.ProjectTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 项目任务 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectTaskMapper extends BaseMapper<ProjectTaskEntity> {

    /**
     * 分页查询任务（含项目名/阶段名/执行人名/代理商公司名）
     *
     * <p>数据权限：ENGINEER 看分配给自己的任务，AGENT_ADMIN 看本公司任务，
     * AGENT_ENGINEER 看分配给自己的任务。多表 JOIN 时必须指定表别名 {@code t}。</p>
     */
    @DataPermission(table = "t", engineerField = "assignee_id",
            agentField = "agent_company_id", agentEngineerField = "agent_engineer_id")
    IPage<ProjectTaskVO> selectTaskPage(IPage<ProjectTaskVO> page, @Param("query") ProjectTaskQueryDTO query);

    /**
     * 按任务ID查询任务详情（含关联名称）
     */
    ProjectTaskVO selectVoById(@Param("id") Long id);

    /**
     * 按项目ID查询任务列表
     */
    List<ProjectTaskEntity> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 按项目ID统计任务状态分布
     */
    List<Map<String, Object>> selectStatusCountByProject(@Param("projectId") Long projectId);

    /**
     * 按项目ID统计指定状态的任务数
     */
    int countByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") String status);

    /**
     * 按项目ID统计未完成任务数（非 COMPLETED / CONFIRMED）
     */
    int countUnfinishedByProject(@Param("projectId") Long projectId);
}
