package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysDictDataQueryDTO;
import com.vibe.system.entity.SysDictDataEntity;
import com.vibe.system.vo.SysDictDataVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictDataEntity> {

    /**
     * 分页查询字典数据
     */
    IPage<SysDictDataVO> selectDictDataPage(IPage<SysDictDataVO> page,
                                            @Param("query") SysDictDataQueryDTO query);

    /**
     * 按 dictType 查询启用的字典数据列表（按 sortOrder 升序）
     */
    List<SysDictDataVO> selectByDictType(@Param("dictType") String dictType);
}
