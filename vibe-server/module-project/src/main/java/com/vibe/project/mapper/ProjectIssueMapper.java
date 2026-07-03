package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectIssueEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目问题 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectIssueMapper extends BaseMapper<ProjectIssueEntity> {

    /**
     * 按项目ID查询问题列表
     */
    List<ProjectIssueEntity> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 查询超期未关闭的问题
     */
    List<ProjectIssueEntity> selectOverdueIssues(@Param("today") LocalDate today);
}
