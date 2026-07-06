package com.vibe.report.service.impl;

import com.vibe.report.dto.DeviceReportQueryDTO;
import com.vibe.report.dto.FinanceReportQueryDTO;
import com.vibe.report.dto.ProjectReportQueryDTO;
import com.vibe.report.dto.ResourceReportQueryDTO;
import com.vibe.report.mapper.DeviceReportMapper;
import com.vibe.report.mapper.FinanceReportMapper;
import com.vibe.report.mapper.ProjectReportMapper;
import com.vibe.report.mapper.ResourceReportMapper;
import com.vibe.report.service.BusinessReportService;
import com.vibe.report.vo.DeviceReportVO;
import com.vibe.report.vo.FinanceReportVO;
import com.vibe.report.vo.ProjectReportVO;
import com.vibe.report.vo.ResourceReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 业务报表服务实现
 *
 * <p>组装项目/设备/资源/财务四类报表，依赖按业务域拆分的 4 个 Mapper：
 * {@link ProjectReportMapper} / {@link DeviceReportMapper} / {@link ResourceReportMapper} / {@link FinanceReportMapper}。</p>
 *
 * <p>高并发聚合查询通过 ES（{@code vibe.es.enabled=true} 时启用）+ Caffeine 二级缓存优化：</p>
 * <ul>
 *   <li>明细查询（含 {@code detail}/{@code byPm}/{@code byEngineer}/{@code byCustomer}）走 MySQL + Caffeine 缓存（5 分钟 TTL）</li>
 *   <li>纯聚合查询（status/productLine/region 分布）在 {@link ManagementCockpitServiceImpl} 走 ES</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessReportServiceImpl implements BusinessReportService {

    private final ProjectReportMapper projectReportMapper;
    private final DeviceReportMapper deviceReportMapper;
    private final ResourceReportMapper resourceReportMapper;
    private final FinanceReportMapper financeReportMapper;

    /**
     * 是否启用 ES 聚合查询（高并发场景走 ES，默认 false 兜底 MySQL）。
     * 通过 {@code vibe.es.enabled} 配置注入。
     */
    @Value("${vibe.es.enabled:false}")
    private boolean esEnabled;

    @Override
    @Cacheable(cacheNames = "reportDetail",
            key = "'projectReport:' + (#query?.status ?: '') + ':' + (#query?.pmId ?: '') + ':' + (#query?.productLine ?: '') + ':' + (#query?.region ?: '')")
    public ProjectReportVO getProjectReport(ProjectReportQueryDTO query) {
        String status = query == null ? null : query.getStatus();
        Long pmId = query == null ? null : query.getPmId();
        String productLine = query == null ? null : query.getProductLine();
        String region = query == null ? null : query.getRegion();

        ProjectReportVO vo = new ProjectReportVO();
        vo.setSummary(projectReportMapper.projectReportSummary(status, pmId, productLine, region));
        vo.setByStatus(projectReportMapper.projectReportByStatus(status, pmId, productLine, region));
        vo.setByProductLine(projectReportMapper.projectReportByProductLine(status, pmId, productLine, region));
        vo.setByRegion(projectReportMapper.projectReportByRegion(status, pmId, productLine, region));
        vo.setByPm(projectReportMapper.projectReportByPm(status, pmId, productLine, region));
        vo.setDetail(projectReportMapper.projectReportDetail(status, pmId, productLine, region));
        return vo;
    }

    @Override
    @Cacheable(cacheNames = "reportDetail",
            key = "'deviceReport:' + (#query?.productLine ?: '')")
    public DeviceReportVO getDeviceReport(DeviceReportQueryDTO query) {
        String productLine = query == null ? null : query.getProductLine();

        DeviceReportVO vo = new DeviceReportVO();
        vo.setSummary(deviceReportMapper.deviceReportSummary(productLine));
        vo.setStatusDistribution(deviceReportMapper.deviceReportByStatus(productLine));
        vo.setProductLineDistribution(deviceReportMapper.deviceReportByProductLine(productLine));
        vo.setBomCompletion(deviceReportMapper.deviceReportBomCompletion());
        vo.setInventoryStatus(deviceReportMapper.deviceReportInventory());
        return vo;
    }

    @Override
    @Cacheable(cacheNames = "reportDetail",
            key = "'resourceReport:' + (#query?.engineerId ?: '')")
    public ResourceReportVO getResourceReport(ResourceReportQueryDTO query) {
        Long engineerId = query == null ? null : query.getEngineerId();

        ResourceReportVO vo = new ResourceReportVO();
        vo.setSummary(resourceReportMapper.resourceReportSummary(engineerId));
        vo.setByEngineer(resourceReportMapper.resourceReportByEngineer(engineerId));
        vo.setByProject(resourceReportMapper.resourceReportByProject());
        return vo;
    }

    @Override
    @Cacheable(cacheNames = "reportDetail",
            key = "'financeReport:' + (#query?.customerId ?: '')")
    public FinanceReportVO getFinanceReport(FinanceReportQueryDTO query) {
        Long customerId = query == null ? null : query.getCustomerId();

        FinanceReportVO vo = new FinanceReportVO();
        vo.setSummary(financeReportMapper.financeReportSummary(customerId));
        vo.setByCustomer(financeReportMapper.financeReportByCustomer(customerId));
        vo.setByRegion(financeReportMapper.financeReportByRegion());
        vo.setByProductLine(financeReportMapper.financeReportByProductLine());
        vo.setAgentSettlement(financeReportMapper.financeReportAgentSettlement());
        return vo;
    }
}
