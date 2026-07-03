package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.device.entity.SparePartEntity;
import com.vibe.device.vo.SparePartVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 备件 Mapper。
 *
 * @author vibe
 */
@Mapper
public interface SparePartMapper extends BaseMapper<SparePartEntity> {

    /**
     * 查询备件台账列表（含仓库名/型号名）。
     */
    List<SparePartVO> selectSparePartList(@Param("keyword") String keyword,
                                          @Param("warehouseId") Long warehouseId,
                                          @Param("modelId") Long modelId);

    /**
     * 按 ID 查询备件详情。
     */
    SparePartVO selectVoById(@Param("id") Long id);
}
