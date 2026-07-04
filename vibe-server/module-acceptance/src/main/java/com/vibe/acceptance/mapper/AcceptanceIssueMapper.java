package com.vibe.acceptance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.acceptance.entity.AcceptanceIssueEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验收遗留问题 Mapper
 *
 * @author vibe
 */
@Mapper
public interface AcceptanceIssueMapper extends BaseMapper<AcceptanceIssueEntity> {
}
