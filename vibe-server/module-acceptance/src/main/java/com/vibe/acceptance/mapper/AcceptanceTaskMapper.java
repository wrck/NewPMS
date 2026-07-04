package com.vibe.acceptance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.acceptance.entity.AcceptanceTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验收任务 Mapper
 *
 * @author vibe
 */
@Mapper
public interface AcceptanceTaskMapper extends BaseMapper<AcceptanceTaskEntity> {
}
