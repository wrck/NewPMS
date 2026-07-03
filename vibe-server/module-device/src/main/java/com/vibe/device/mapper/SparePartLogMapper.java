package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.device.entity.SparePartLogEntity;
import com.vibe.device.vo.SparePartLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 备件领用/归还记录 Mapper。
 *
 * @author vibe
 */
@Mapper
public interface SparePartLogMapper extends BaseMapper<SparePartLogEntity> {

    /**
     * 查询备件操作流水（含备件名/项目名）。
     */
    List<SparePartLogVO> selectLogList(@Param("sparePartId") Long sparePartId,
                                       @Param("projectId") Long projectId,
                                       @Param("actionType") String actionType);
}
