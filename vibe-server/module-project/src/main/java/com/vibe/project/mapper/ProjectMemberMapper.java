package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectMemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目成员 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMemberEntity> {

    /**
     * 按项目ID查询成员列表
     */
    List<ProjectMemberEntity> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 按项目ID与用户ID查询成员（用于校验是否项目成员）
     */
    ProjectMemberEntity selectByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 按用户ID查询其参与的项目ID列表
     */
    List<Long> selectProjectIdsByUserId(@Param("userId") Long userId);

    /**
     * 按项目ID删除全部成员（逻辑删除）
     */
    int deleteByProjectId(@Param("projectId") Long projectId);
}
