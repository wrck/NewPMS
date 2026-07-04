package com.vibe.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.integration.entity.IntegrationConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 集成配置 Mapper
 *
 * @author vibe
 */
@Mapper
public interface IntegrationConfigMapper extends BaseMapper<IntegrationConfigEntity> {
}
