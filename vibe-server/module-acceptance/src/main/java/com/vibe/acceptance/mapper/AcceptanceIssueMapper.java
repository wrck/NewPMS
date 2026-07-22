package com.vibe.acceptance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.acceptance.dto.AcceptanceIssueQueryDTO;
import com.vibe.acceptance.entity.AcceptanceIssueEntity;
import com.vibe.acceptance.vo.AcceptanceIssueVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 验收遗留问题 Mapper
 *
 * @author vibe
 */
@Mapper
public interface AcceptanceIssueMapper extends BaseMapper<AcceptanceIssueEntity> {

    /**
     * 分页查询遗留问题（含项目名）
     */
    IPage<AcceptanceIssueVO> selectIssuePage(IPage<AcceptanceIssueVO> page, @Param("query") AcceptanceIssueQueryDTO query);

    /**
     * 按问题ID查询遗留问题详情（含关联名称）
     */
    AcceptanceIssueVO selectVoById(@Param("id") Long id);
}
