package com.vibe.project.service;

import com.vibe.project.dto.ProjectCommentDTO;
import com.vibe.project.vo.ProjectCommentVO;

import java.util.List;

/**
 * 项目沟通记录服务
 *
 * @author vibe
 */
public interface ProjectCommentService {

    /**
     * 查询项目下的全部评论（含回复树结构）
     */
    List<ProjectCommentVO> listByProjectId(Long projectId);

    /**
     * 查询任务下的全部评论
     */
    List<ProjectCommentVO> listByTaskId(Long taskId);

    /**
     * 发表评论（自动填充当前登录人为作者，支持回复 parentId）
     */
    Long create(ProjectCommentDTO dto);

    /**
     * 删除评论（仅作者或管理员可删除）
     */
    void delete(Long id);
}

