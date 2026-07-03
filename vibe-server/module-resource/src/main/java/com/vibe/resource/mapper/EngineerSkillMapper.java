package com.vibe.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.resource.entity.EngineerSkillEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工程师技能 Mapper
 *
 * @author vibe
 */
@Mapper
public interface EngineerSkillMapper extends BaseMapper<EngineerSkillEntity> {

    /**
     * 按工程师ID查询技能列表
     */
    List<EngineerSkillEntity> selectByEngineerId(@Param("engineerId") Long engineerId);

    /**
     * 按工程师ID列表批量查询技能
     */
    List<EngineerSkillEntity> selectByEngineerIds(@Param("engineerIds") List<Long> engineerIds);

    /**
     * 按工程师ID逻辑删除全部技能
     */
    int deleteByEngineerId(@Param("engineerId") Long engineerId);
}
