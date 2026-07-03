package com.vibe.project.service;

import com.vibe.common.result.PageResult;
import com.vibe.project.dto.ProjectTemplateDTO;
import com.vibe.project.dto.ProjectTemplatePhaseDTO;
import com.vibe.project.dto.ProjectTemplateQueryDTO;
import com.vibe.project.dto.ProjectTemplateTaskDTO;
import com.vibe.project.vo.ProjectTemplateDetailVO;
import com.vibe.project.vo.ProjectTemplateVO;

/**
 * 项目模板服务
 *
 * <p>核心业务：模板 CRUD + 阶段/任务 CRUD。模板用于立项时一键生成项目阶段与任务。</p>
 *
 * @author vibe
 */
public interface ProjectTemplateService {

    /**
     * 分页查询模板
     */
    PageResult<ProjectTemplateVO> page(ProjectTemplateQueryDTO query);

    /**
     * 模板详情（含阶段与任务）
     */
    ProjectTemplateDetailVO getDetail(Long id);

    /**
     * 新增模板
     */
    Long create(ProjectTemplateDTO dto);

    /**
     * 编辑模板基础信息
     */
    void update(ProjectTemplateDTO dto);

    /**
     * 删除模板（连同阶段与任务逻辑删除）
     */
    void delete(Long id);

    /* ============ 阶段 ============ */

    /**
     * 新增模板阶段
     */
    Long addPhase(ProjectTemplatePhaseDTO dto);

    /**
     * 编辑模板阶段
     */
    void updatePhase(ProjectTemplatePhaseDTO dto);

    /**
     * 删除模板阶段
     */
    void deletePhase(Long id);

    /* ============ 任务 ============ */

    /**
     * 新增模板任务
     */
    Long addTask(ProjectTemplateTaskDTO dto);

    /**
     * 编辑模板任务
     */
    void updateTask(ProjectTemplateTaskDTO dto);

    /**
     * 删除模板任务
     */
    void deleteTask(Long id);
}

