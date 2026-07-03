package com.vibe.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.device.dto.DeviceInstanceQueryDTO;
import com.vibe.device.entity.DeviceInstanceEntity;
import com.vibe.device.enums.DeviceStatus;
import com.vibe.device.mapper.DeviceInstanceMapper;
import com.vibe.device.service.DeviceBomService;
import com.vibe.device.service.DeviceDashboardService;
import com.vibe.device.vo.DeviceBomVO;
import com.vibe.device.vo.DeviceDashboardVO;
import com.vibe.device.vo.DeviceInstanceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备看板服务实现。
 *
 * <p>聚合设备状态分布、型号维度统计、BOM 完成率、异常设备列表，
 * 支持项目维度（projectId 非空）与全局维度（projectId 为空）两种视图。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDashboardServiceImpl implements DeviceDashboardService {

    /** 异常设备单次查询上限 */
    private static final long ABNORMAL_MAX = 200L;

    private final DeviceInstanceMapper deviceInstanceMapper;
    private final DeviceBomService deviceBomService;

    @Override
    public DeviceDashboardVO getDashboard(Long projectId) {
        DeviceDashboardVO vo = new DeviceDashboardVO();
        vo.setProjectId(projectId);

        // 设备总数（按项目维度过滤）
        vo.setTotalDevices(countDevices(projectId));

        // BOM 完成率统计（项目维度，全局看板时为空）
        if (projectId != null) {
            List<DeviceBomVO> bomProgress = deviceBomService.statProgress(projectId);
            vo.setBomProgress(bomProgress);
        }

        // 设备状态分布
        vo.setStatusDistribution(deviceInstanceMapper.selectStatusDistribution(projectId));

        // 按型号统计
        vo.setCountByModel(deviceInstanceMapper.selectCountByModel(projectId));

        // 异常设备列表（DAMAGED/LOST/REPAIR）
        vo.setAbnormalDevices(queryAbnormalDevices(projectId));

        return vo;
    }

    /**
     * 统计设备总数（按项目维度过滤）。
     */
    private long countDevices(Long projectId) {
        LambdaQueryWrapper<DeviceInstanceEntity> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(DeviceInstanceEntity::getProjectId, projectId);
        }
        return deviceInstanceMapper.selectCount(wrapper);
    }

    /**
     * 查询异常设备列表（DAMAGED/LOST/REPAIR 状态）。
     *
     * <p>复用 selectInstancePage 关联查询（含型号名/项目名/仓库名），
     * 受 @DataPermission 数据权限控制。分状态查询后合并，单状态上限 200 条。</p>
     */
    private List<DeviceInstanceVO> queryAbnormalDevices(Long projectId) {
        List<DeviceInstanceVO> result = new ArrayList<>();
        DeviceStatus[] abnormalStatuses = {DeviceStatus.DAMAGED, DeviceStatus.LOST, DeviceStatus.REPAIR};
        for (DeviceStatus status : abnormalStatuses) {
            DeviceInstanceQueryDTO query = new DeviceInstanceQueryDTO();
            query.setStatus(status.name());
            query.setProjectId(projectId);
            query.setPage(1);
            query.setSize((int) ABNORMAL_MAX);
            IPage<DeviceInstanceVO> page = new Page<>(1, ABNORMAL_MAX);
            IPage<DeviceInstanceVO> result0 = deviceInstanceMapper.selectInstancePage(page, query);
            result.addAll(result0.getRecords());
        }
        return result;
    }
}
