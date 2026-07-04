package com.vibe.acceptance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.acceptance.entity.AcceptanceTestRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验收测试记录 Mapper
 *
 * @author vibe
 */
@Mapper
public interface AcceptanceTestRecordMapper extends BaseMapper<AcceptanceTestRecordEntity> {
}
