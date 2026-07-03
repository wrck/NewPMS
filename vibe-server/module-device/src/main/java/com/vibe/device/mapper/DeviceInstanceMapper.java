package com.vibe.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.annotation.DataPermission;
import com.vibe.device.dto.DeviceInstanceQueryDTO;
import com.vibe.device.entity.DeviceInstanceEntity;
import com.vibe.device.vo.DeviceInstanceVO;
import com.vibe.device.vo.DeviceStatusLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 设备实例 Mapper。
 *
 * @author vibe
 */
@Mapper
public interface DeviceInstanceMapper extends BaseMapper<DeviceInstanceEntity> {

    /**
     * 分页查询设备实例（关联型号名/项目名/仓库名）。
     *
     * <p>数据权限：PM 仅看自己负责项目下的设备（通过 project.pm_id 过滤）；
     * 客户看自己关联项目的设备。多表 JOIN 指定别名 {@code p} 避免字段歧义。
     * Phase 1 简化：ENGINEER 设备可见性按 PM 维度（engineerField=pm_id），
     * 后续可通过 project_member 关联细化。</p>
     */
    @DataPermission(table = "p", pmField = "pm_id", customerField = "customer_id", engineerField = "pm_id")
    IPage<DeviceInstanceVO> selectInstancePage(IPage<DeviceInstanceVO> page,
                                                @Param("query") DeviceInstanceQueryDTO query);

    /**
     * 按 ID 查询设备详情（含型号名/项目名/仓库名）。
     */
    DeviceInstanceVO selectVoById(@Param("id") Long id);

    /**
     * 查询设备状态变更轨迹（按时间倒序）。
     */
    List<DeviceStatusLogVO> selectStatusTrail(@Param("deviceId") Long deviceId);

    /**
     * 设备状态分布统计（按 status 分组计数）。
     */
    List<Map<String, Object>> selectStatusDistribution(@Param("projectId") Long projectId);

    /**
     * 按型号维度统计设备数量（用于看板）。
     */
    List<Map<String, Object>> selectCountByModel(@Param("projectId") Long projectId);

    /**
     * 统计项目中各状态设备数量。
     */
    List<Map<String, Object>> selectCountByStatusInProject(@Param("projectId") Long projectId);
}
