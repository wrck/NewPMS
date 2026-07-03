package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.device.entity.DeviceBomEntity;
import com.vibe.device.vo.DeviceBomVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目设备清单（BOM）Mapper。
 *
 * @author vibe
 */
@Mapper
public interface DeviceBomMapper extends BaseMapper<DeviceBomEntity> {

    /**
     * 查询项目 BOM 列表（含型号信息与进度数量）。
     */
    List<DeviceBomVO> selectBomListByProject(@Param("projectId") Long projectId);

    /**
     * 按 项目ID + 型号ID 查询 BOM（唯一）。
     */
    DeviceBomEntity selectByProjectAndModel(@Param("projectId") Long projectId,
                                            @Param("modelId") Long modelId);

    /**
     * 按 项目ID + 型号ID 查询 BOM 详情（含型号信息）。
     */
    DeviceBomVO selectVoById(@Param("id") Long id);
}
