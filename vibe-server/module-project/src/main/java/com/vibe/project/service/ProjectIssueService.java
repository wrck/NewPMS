package com.vibe.project.service;

import com.vibe.project.dto.ProjectIssueDTO;
import com.vibe.project.vo.ProjectIssueVO;

import java.util.List;

/**
 * 项目问题服务
 *
 * <p>核心业务：问题登记、状态流转、超期升级。</p>
 *
 * @author vibe
 */
public interface ProjectIssueService {

    /**
     * 查询项目下的全部问题
     */
    List<ProjectIssueVO> listByProjectId(Long projectId);

    /**
     * 登记问题
     */
    Long create(ProjectIssueDTO dto);

    /**
     * 编辑问题
     */
    void update(ProjectIssueDTO dto);

    /**
     * 删除问题
     */
    void delete(Long id);

    /**
     * 问题详情
     */
    ProjectIssueVO getDetail(Long id);

    /**
     * 状态流转（OPEN→PROCESSING→RESOLVED→CLOSED），RESOLVED 时自动记录解决时间
     */
    void transition(Long id, String targetStatus);

    /**
     * 查询全部超期未关闭的问题（定时任务调度用）
     */
    List<ProjectIssueVO> listOverdue();
}

