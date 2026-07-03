package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.device.entity.DeviceStatusLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备状态变更日志 Mapper。
 *
 * @author vibe
 */
@Mapper
public interface DeviceStatusLogMapper extends BaseMapper<DeviceStatusLogEntity> {

    /**
     * 查询设备状态变更轨迹（按时间倒序）。
     */
    List<DeviceStatusLogEntity> selectLogListByDevice(@Param("deviceId") Long deviceId);
}
