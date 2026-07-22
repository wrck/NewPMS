package com.vibe.acceptance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.acceptance.dto.AcceptanceDocQueryDTO;
import com.vibe.acceptance.entity.AcceptanceDocEntity;
import com.vibe.acceptance.vo.AcceptanceDocVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 竣工文档 Mapper
 *
 * @author vibe
 */
@Mapper
public interface AcceptanceDocMapper extends BaseMapper<AcceptanceDocEntity> {

    /**
     * 分页查询竣工文档（含项目名）
     */
    IPage<AcceptanceDocVO> selectDocPage(IPage<AcceptanceDocVO> page, @Param("query") AcceptanceDocQueryDTO query);

    /**
     * 按文档ID查询竣工文档详情（含关联名称）
     */
    AcceptanceDocVO selectVoById(@Param("id") Long id);
}
