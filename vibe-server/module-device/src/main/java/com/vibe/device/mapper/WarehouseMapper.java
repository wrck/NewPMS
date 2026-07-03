package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.device.entity.WarehouseEntity;
import com.vibe.device.vo.WarehouseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仓库 Mapper。
 *
 * @author vibe
 */
@Mapper
public interface WarehouseMapper extends BaseMapper<WarehouseEntity> {

    /**
     * 查询仓库列表（含管理员名）。
     */
    List<WarehouseVO> selectWarehouseList(@Param("keyword") String keyword,
                                          @Param("region") String region);

    /**
     * 按 ID 查询仓库详情。
     */
    WarehouseVO selectVoById(@Param("id") Long id);
}
