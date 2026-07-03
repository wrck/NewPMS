package com.vibe.project.service;

import com.vibe.common.result.PageResult;
import com.vibe.project.dto.ProjectArchiveDTO;
import com.vibe.project.dto.ProjectCreateDTO;
import com.vibe.project.dto.ProjectQueryDTO;
import com.vibe.project.dto.ProjectStatusDTO;
import com.vibe.project.dto.ProjectUpdateDTO;
import com.vibe.project.vo.ProjectDetailVO;
import com.vibe.project.vo.ProjectGanttVO;
import com.vibe.project.vo.ProjectKanbanVO;
import com.vibe.project.vo.ProjectVO;

import java.util.List;

/**
 * 项目服务
 *
 * <p>核心业务：立项 / 状态机管理 / 编辑删除 / 多维度查询 / 看板分组 /
 * 甘特图数据 / 详情聚合 / 结项检查 / 归档。</p>
 *
 * @author vibe
 */
public interface ProjectService {

    /**
     * 立项（手动创建 / 选择模板生成阶段与任务 / 项目编号 PRJ-YYYYMM-XXX 自动生成）
     */
    Long create(ProjectCreateDTO dto);

    /**
     * 编辑项目
     */
    void update(ProjectUpdateDTO dto);

    /**
     * 删除项目（仅 INIT/PLAN 状态可删除）
     */
    void delete(Long id);

    /**
     * 分页查询（多维度筛选排序）
     */
    PageResult<ProjectVO> page(ProjectQueryDTO query);

    /**
     * 项目详情聚合（含阶段、里程碑、成员、任务统计、风险问题计数）
     */
    ProjectDetailVO getDetail(Long id);

    /**
     * 状态机流转（含乐观锁校验，非法流转抛 40902 业务冲突）
     */
    void transition(ProjectStatusDTO dto);

    /**
     * 看板分组查询（按状态分组）
     */
    List<ProjectKanbanVO> kanban(ProjectQueryDTO query);

    /**
     * 甘特图数据
     */
    ProjectGanttVO gantt(Long id);

    /**
     * 结项检查（所有任务完成 / 文档归档等前置校验）
     *
     * @return 检查通过返回 null；不通过返回不满足原因描述
     */
    String checkClose(Long id);

    /**
     * 归档（CLOSE → ARCHIVED，含复盘记录）
     */
    void archive(Long id, ProjectArchiveDTO dto);
}
