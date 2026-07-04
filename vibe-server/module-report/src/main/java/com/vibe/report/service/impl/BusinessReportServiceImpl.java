package com.vibe.report.service.impl;

import com.vibe.report.dto.DeviceReportQueryDTO;
import com.vibe.report.dto.FinanceReportQueryDTO;
import com.vibe.report.dto.ProjectReportQueryDTO;
import com.vibe.report.dto.ResourceReportQueryDTO;
import com.vibe.report.mapper.ReportMapper;
import com.vibe.report.service.BusinessReportService;
import com.vibe.report.vo.DeviceReportVO;
import com.vibe.report.vo.FinanceReportVO;
import com.vibe.report.vo.ProjectReportVO;
import com.vibe.report.vo.ResourceReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务报表服务实现
 *
 * <p>组装项目/设备/资源/财务四类报表，依赖 ReportMapper 提供的聚合 SQL。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessReportServiceImpl implements BusinessReportService {

    private final ReportMapper reportMapper;

    @Override
    public ProjectReportVO getProjectReport(ProjectReportQueryDTO query) {
        String status = query == null ? null : query.getStatus();
        Long pmId = query == null ? null : query.getPmId();
        String productLine = query == null ? null : query.getProductLine();
        String region = query == null ? null : query.getRegion();

        ProjectReportVO vo = new ProjectReportVO();
        vo.setSummary(reportMapper.projectReportSummary(status, pmId, productLine, region));
        vo.setByStatus(reportMapper.projectReportByStatus(status, pmId, productLine, region));
        vo.setByProductLine(reportMapper.projectReportByProductLine(status, pmId, productLine, region));
        vo.setByRegion(reportMapper.projectReportByRegion(status, pmId, productLine, region));
        vo.setByPm(reportMapper.projectReportByPm(status, pmId, productLine, region));
        vo.setDetail(reportMapper.projectReportDetail(status, pmId, productLine, region));
        return vo;
    }

    @Override
    public DeviceReportVO getDeviceReport(DeviceReportQueryDTO query) {
        String productLine = query == null ? null : query.getProductLine();

        DeviceReportVO vo = new DeviceReportVO();
        vo.setSummary(reportMapper.deviceReportSummary(productLine));
        vo.setStatusDistribution(reportMapper.deviceReportByStatus(productLine));
        vo.setProductLineDistribution(reportMapper.deviceReportByProductLine(productLine));
        vo.setBomCompletion(reportMapper.deviceReportBomCompletion());
        vo.setInventoryStatus(reportMapper.deviceReportInventory());
        return vo;
    }

    @Override
    public ResourceReportVO getResourceReport(ResourceReportQueryDTO query) {
        Long engineerId = query == null ? null : query.getEngineerId();

        ResourceReportVO vo = new ResourceReportVO();
        vo.setSummary(reportMapper.resourceReportSummary(engineerId));
        vo.setByEngineer(reportMapper.resourceReportByEngineer(engineerId));
        vo.setByProject(reportMapper.resourceReportByProject());
        return vo;
    }

    @Override
    public FinanceReportVO getFinanceReport(FinanceReportQueryDTO query) {
        Long customerId = query == null ? null : query.getCustomerId();

        FinanceReportVO vo = new FinanceReportVO();
        vo.setSummary(reportMapper.financeReportSummary(customerId));
        vo.setByCustomer(reportMapper.financeReportByCustomer(customerId));
        vo.setByRegion(reportMapper.financeReportByRegion());
        vo.setByProductLine(reportMapper.financeReportByProductLine());
        vo.setAgentSettlement(reportMapper.financeReportAgentSettlement());
        return vo;
    }
}
