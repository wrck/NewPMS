package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectChangeLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目变更记录 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectChangeLogMapper extends BaseMapper<ProjectChangeLogEntity> {

    /**
     * 按项目ID查询变更记录列表（倒序）
     */
    List<ProjectChangeLogEntity> selectByProjectId(@Param("projectId") Long projectId);
}
