package com.vibe.project.service;

import com.vibe.project.dto.ProjectRiskDTO;
import com.vibe.project.vo.ProjectRiskVO;

import java.util.List;

/**
 * 项目风险服务
 *
 * <p>核心业务：风险登记、状态流转、超期升级。</p>
 *
 * @author vibe
 */
public interface ProjectRiskService {

    /**
     * 查询项目下的全部风险
     */
    List<ProjectRiskVO> listByProjectId(Long projectId);

    /**
     * 登记风险
     */
    Long create(ProjectRiskDTO dto);

    /**
     * 编辑风险
     */
    void update(ProjectRiskDTO dto);

    /**
     * 删除风险
     */
    void delete(Long id);

    /**
     * 风险详情
     */
    ProjectRiskVO getDetail(Long id);

    /**
     * 状态流转（OPEN→PROCESSING→RESOLVED→CLOSED）
     */
    void transition(Long id, String targetStatus);

    /**
     * 查询全部超期未关闭的风险（定时任务调度用）
     */
    List<ProjectRiskVO> listOverdue();
}

