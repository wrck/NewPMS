package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.entity.SysPositionEntity;
import com.vibe.system.vo.SysPositionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 岗位 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysPositionMapper extends BaseMapper<SysPositionEntity> {

    /**
     * 分页查询岗位（含组织名称）
     */
    IPage<SysPositionVO> selectPositionPage(IPage<SysPositionVO> page,
                                            @Param("keyword") String keyword,
                                            @Param("orgId") Long orgId);

    /**
     * 按 ID 查询岗位（含组织名称）
     */
    SysPositionVO selectVoById(@Param("id") Long id);
}
