package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectTemplatePhaseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目模板阶段 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectTemplatePhaseMapper extends BaseMapper<ProjectTemplatePhaseEntity> {

    /**
     * 按模板ID查询阶段列表
     */
    List<ProjectTemplatePhaseEntity> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 按模板ID删除全部阶段（逻辑删除）
     */
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
