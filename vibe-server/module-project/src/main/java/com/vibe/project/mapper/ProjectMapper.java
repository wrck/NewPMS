package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.annotation.DataPermission;
import com.vibe.project.dto.ProjectQueryDTO;
import com.vibe.project.entity.ProjectEntity;
import com.vibe.project.vo.ProjectGanttVO;
import com.vibe.project.vo.ProjectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 项目 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectMapper extends BaseMapper<ProjectEntity> {

    /**
     * 分页查询项目（含客户名/PM名关联，含多维度筛选）
     *
     * <p>数据权限：PM 看自己负责的项目，代理商看本公司项目，客户看自己关联项目。
     * 通过 {@link DataPermission} 注解由数据权限拦截器自动追加 WHERE 条件，
     * 多表 JOIN 时必须指定表别名 {@code p}，避免字段歧义。</p>
     */
    @DataPermission(table = "p", pmField = "pm_id", customerField = "customer_id")
    IPage<ProjectVO> selectProjectPage(IPage<ProjectVO> page, @Param("query") ProjectQueryDTO query);

    /**
     * 按项目ID查询项目详情（含客户名/PM名）
     */
    ProjectVO selectVoById(@Param("id") Long id);

    /**
     * 看板分组查询：按状态统计项目数量与项目列表（受数据权限控制）
     */
    @DataPermission(table = "p", pmField = "pm_id", customerField = "customer_id")
    List<Map<String, Object>> selectKanbanGroups(@Param("query") ProjectQueryDTO query);

    /**
     * 查询项目甘特图数据（含阶段与任务）
     */
    ProjectGanttVO selectGanttById(@Param("id") Long id);

    /**
     * 查询项目甘特图阶段数据
     */
    List<ProjectGanttVO.PhaseGantt> selectGanttPhases(@Param("projectId") Long projectId);

    /**
     * 查询项目甘特图任务数据（含执行人姓名）
     */
    List<ProjectGanttVO.TaskGantt> selectGanttTasks(@Param("projectId") Long projectId);
}
