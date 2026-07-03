package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.device.entity.DeviceInventoryLogEntity;
import com.vibe.device.vo.DeviceInventoryLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备出入库记录 Mapper。
 *
 * @author vibe
 */
@Mapper
public interface DeviceInventoryLogMapper extends BaseMapper<DeviceInventoryLogEntity> {

    /**
     * 查询设备的出入库历史（含仓库/项目名）。
     */
    List<DeviceInventoryLogVO> selectLogListByDevice(@Param("deviceId") Long deviceId);

    /**
     * 分页查询出入库流水（按仓库/项目/操作类型筛选）。
     */
    List<DeviceInventoryLogVO> selectLogList(@Param("warehouseId") Long warehouseId,
                                             @Param("projectId") Long projectId,
                                             @Param("actionType") String actionType);
}
