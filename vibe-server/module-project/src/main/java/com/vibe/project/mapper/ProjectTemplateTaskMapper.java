package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectTemplateTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目模板任务 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectTemplateTaskMapper extends BaseMapper<ProjectTemplateTaskEntity> {

    /**
     * 按模板ID查询任务列表
     */
    List<ProjectTemplateTaskEntity> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 按模板ID删除全部任务（逻辑删除）
     */
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
