package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.project.dto.ProjectTemplateQueryDTO;
import com.vibe.project.entity.ProjectTemplateEntity;
import com.vibe.project.vo.ProjectTemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 项目模板 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectTemplateMapper extends BaseMapper<ProjectTemplateEntity> {

    /**
     * 分页查询项目模板
     */
    IPage<ProjectTemplateVO> selectTemplatePage(IPage<ProjectTemplateVO> page,
                                                @Param("query") ProjectTemplateQueryDTO query);
}
