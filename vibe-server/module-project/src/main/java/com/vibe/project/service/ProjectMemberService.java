package com.vibe.project.service;

import com.vibe.project.vo.ProjectMemberVO;

import java.util.List;

/**
 * 项目成员服务
 *
 * @author vibe
 */
public interface ProjectMemberService {

    /**
     * 查询项目下的全部成员
     */
    List<ProjectMemberVO> listByProjectId(Long projectId);

    /**
     * 添加成员（重复加入幂等）
     */
    Long addMember(Long projectId, Long userId, String role);

    /**
     * 修改成员角色
     */
    void updateRole(Long id, String role);

    /**
     * 移除成员
     */
    void remove(Long id);

    /**
     * 校验当前用户是否项目成员
     *
     * @return 成员实体存在返回 true
     */
    boolean isMember(Long projectId, Long userId);

    /**
     * 查询用户参与的项目ID列表
     */
    List<Long> listProjectIdsByUserId(Long userId);
}

