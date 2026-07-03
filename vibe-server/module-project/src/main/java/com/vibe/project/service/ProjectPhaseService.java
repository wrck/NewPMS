package com.vibe.project.service;

import com.vibe.project.dto.ProjectPhaseDTO;
import com.vibe.project.vo.ProjectPhaseVO;

import java.util.List;

/**
 * 项目阶段服务
 *
 * <p>核心业务：阶段增删改、时间范围维护、交付物清单管理、状态流转。</p>
 *
 * @author vibe
 */
public interface ProjectPhaseService {

    /**
     * 查询项目下的全部阶段（按 sortOrder 升序）
     */
    List<ProjectPhaseVO> listByProjectId(Long projectId);

    /**
     * 新增阶段
     */
    Long create(ProjectPhaseDTO dto);

    /**
     * 编辑阶段（含时间范围、交付物清单）
     */
    void update(ProjectPhaseDTO dto);

    /**
     * 删除阶段
     */
    void delete(Long id);

    /**
     * 阶段详情
     */
    ProjectPhaseVO getDetail(Long id);
}

