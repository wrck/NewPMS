package com.vibe.report.service;

import com.vibe.report.dto.DeviceReportQueryDTO;
import com.vibe.report.dto.FinanceReportQueryDTO;
import com.vibe.report.dto.ProjectReportQueryDTO;
import com.vibe.report.dto.ResourceReportQueryDTO;
import com.vibe.report.vo.DeviceReportVO;
import com.vibe.report.vo.FinanceReportVO;
import com.vibe.report.vo.ProjectReportVO;
import com.vibe.report.vo.ResourceReportVO;

/**
 * 业务报表服务
 *
 * <p>提供项目、设备、资源、财务四类业务报表数据组装。</p>
 *
 * @author vibe
 */
public interface BusinessReportService {

    /**
     * 项目报表
     */
    ProjectReportVO getProjectReport(ProjectReportQueryDTO query);

    /**
     * 设备报表
     */
    DeviceReportVO getDeviceReport(DeviceReportQueryDTO query);

    /**
     * 资源报表
     */
    ResourceReportVO getResourceReport(ResourceReportQueryDTO query);

    /**
     * 财务报表
     */
    FinanceReportVO getFinanceReport(FinanceReportQueryDTO query);
}
