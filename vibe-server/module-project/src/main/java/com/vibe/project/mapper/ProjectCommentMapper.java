package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectCommentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目评论 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectCommentMapper extends BaseMapper<ProjectCommentEntity> {

    /**
     * 按项目ID查询评论列表（含评论人姓名）
     */
    List<com.vibe.project.vo.ProjectCommentVO> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 按任务ID查询评论列表
     */
    List<com.vibe.project.vo.ProjectCommentVO> selectByTaskId(@Param("taskId") Long taskId);
}
