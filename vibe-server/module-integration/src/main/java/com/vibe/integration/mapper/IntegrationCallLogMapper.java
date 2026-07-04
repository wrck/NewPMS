package com.vibe.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.integration.entity.IntegrationCallLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 集成调用日志 Mapper
 *
 * @author vibe
 */
@Mapper
public interface IntegrationCallLogMapper extends BaseMapper<IntegrationCallLogEntity> {
}
