package com.vibe.device.service;

import com.vibe.device.vo.DeviceDashboardVO;

/**
 * 设备看板服务。
 *
 * <p>提供项目维度（或全局）的设备状态看板：BOM 完成率、状态分布、
 * 按型号统计、异常设备列表等多维统计。</p>
 *
 * @author vibe
 */
public interface DeviceDashboardService {

    /**
     * 获取设备看板数据。
     *
     * @param projectId 项目ID（可空，空表示全局看板）
     */
    DeviceDashboardVO getDashboard(Long projectId);
}
