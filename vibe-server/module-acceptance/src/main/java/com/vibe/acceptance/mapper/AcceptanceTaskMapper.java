package com.vibe.acceptance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.acceptance.dto.AcceptanceTaskQueryDTO;
import com.vibe.acceptance.entity.AcceptanceTaskEntity;
import com.vibe.acceptance.vo.AcceptanceTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 验收任务 Mapper
 *
 * @author vibe
 */
@Mapper
public interface AcceptanceTaskMapper extends BaseMapper<AcceptanceTaskEntity> {

    /**
     * 分页查询验收任务（含项目名/验收标准名）
     */
    IPage<AcceptanceTaskVO> selectTaskPage(IPage<AcceptanceTaskVO> page, @Param("query") AcceptanceTaskQueryDTO query);

    /**
     * 按任务ID查询验收任务详情（含关联名称）
     */
    AcceptanceTaskVO selectVoById(@Param("id") Long id);
}
