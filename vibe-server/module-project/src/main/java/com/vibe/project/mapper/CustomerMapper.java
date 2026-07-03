package com.vibe.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.project.entity.CustomerEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户 Mapper
 *
 * @author vibe
 */
@Mapper
public interface CustomerMapper extends BaseMapper<CustomerEntity> {
}
