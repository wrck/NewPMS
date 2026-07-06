package com.vibe.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.collaboration.entity.CustomerSessionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户会话 Mapper
 *
 * @author vibe
 */
@Mapper
public interface CustomerSessionMapper extends BaseMapper<CustomerSessionEntity> {
}
