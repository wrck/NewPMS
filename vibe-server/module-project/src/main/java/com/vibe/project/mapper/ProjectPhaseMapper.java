package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectPhaseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目阶段 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectPhaseMapper extends BaseMapper<ProjectPhaseEntity> {

    /**
     * 按项目ID查询阶段列表（按排序升序）
     */
    List<ProjectPhaseEntity> selectByProjectId(@Param("projectId") Long projectId);
}
