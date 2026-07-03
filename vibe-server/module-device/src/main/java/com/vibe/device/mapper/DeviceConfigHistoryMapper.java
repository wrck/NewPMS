package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.device.entity.DeviceConfigHistoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备配置历史 Mapper。
 *
 * @author vibe
 */
@Mapper
public interface DeviceConfigHistoryMapper extends BaseMapper<DeviceConfigHistoryEntity> {

    /**
     * 查询设备配置变更历史（按版本号倒序）。
     */
    List<DeviceConfigHistoryEntity> selectHistoryByDevice(@Param("deviceId") Long deviceId);
}
