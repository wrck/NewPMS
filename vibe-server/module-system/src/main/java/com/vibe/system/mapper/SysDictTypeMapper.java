package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysDictTypeQueryDTO;
import com.vibe.system.entity.SysDictTypeEntity;
import com.vibe.system.vo.SysDictTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典类型 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictTypeEntity> {

    /**
     * 分页查询字典类型
     */
    IPage<SysDictTypeVO> selectDictTypePage(IPage<SysDictTypeVO> page,
                                            @Param("query") SysDictTypeQueryDTO query);

    /**
     * 按 dictType 查询字典类型
     */
    SysDictTypeEntity selectByDictType(@Param("dictType") String dictType);
}
