package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.ProjectRiskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目风险 Mapper
 *
 * @author vibe
 */
@Mapper
public interface ProjectRiskMapper extends BaseMapper<ProjectRiskEntity> {

    /**
     * 按项目ID查询风险列表
     */
    List<ProjectRiskEntity> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 查询超期未关闭的风险（截止日期早于今天且状态非 CLOSED/RESOLVED）
     */
    List<ProjectRiskEntity> selectOverdueRisks(@Param("today") LocalDate today);
}
