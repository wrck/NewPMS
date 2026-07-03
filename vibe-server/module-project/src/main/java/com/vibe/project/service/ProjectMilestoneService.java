package com.vibe.project.service;

import com.vibe.project.dto.ProjectMilestoneDTO;
import com.vibe.project.vo.ProjectMilestoneVO;

import java.util.List;

/**
 * 项目里程碑服务
 *
 * @author vibe
 */
public interface ProjectMilestoneService {

    /**
     * 查询项目下的全部里程碑
     */
    List<ProjectMilestoneVO> listByProjectId(Long projectId);

    /**
     * 新增里程碑
     */
    Long create(ProjectMilestoneDTO dto);

    /**
     * 编辑里程碑
     */
    void update(ProjectMilestoneDTO dto);

    /**
     * 删除里程碑
     */
    void delete(Long id);

    /**
     * 里程碑详情
     */
    ProjectMilestoneVO getDetail(Long id);

    /**
     * 标记里程碑达成（设置实际日期与 REACHED 状态）
     */
    void markReached(Long id, java.time.LocalDate actualDate);
}

