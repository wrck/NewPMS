package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectMilestoneEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目里程碑 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectMilestoneMapper extends BaseMapper<ProjectMilestoneEntity> {

    /**
     * 按项目ID查询里程碑列表
     */
    List<ProjectMilestoneEntity> selectByProjectId(@Param("projectId") Long projectId);
}
