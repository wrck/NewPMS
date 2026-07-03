package com.vibe.report.service.impl;

import com.vibe.report.constant.ReportConstant;
import com.vibe.report.mapper.ReportMapper;
import com.vibe.report.service.ManagementCockpitService;
import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.CockpitStatVO;
import com.vibe.report.vo.RiskProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * 管理驾驶舱服务实现
 *
 * <p>核心指标卡片含环比数据（本月 vs 上月），环比增长率计算公式：
 * <pre>(本月数 - 上月数) / 上月数 * 100</pre>
 * 上月数为 0 时，本月有数据则记 100%，无数据则记 0%。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagementCockpitServiceImpl implements ManagementCockpitService {

    private final ReportMapper reportMapper;

    @Override
    public CockpitStatVO getStats() {
        CockpitStatVO vo = new CockpitStatVO();

        // ===== 当期总数 =====
        vo.setProjectCount(nvl(reportMapper.countAllProjects()));
        vo.setDeviceCount(nvl(reportMapper.countAllDevices()));
        vo.setEngineerCount(nvl(reportMapper.countAllEngineers()));
        vo.setAgentCompanyCount(nvl(reportMapper.countAllAgentCompanies()));

        // ===== 活跃数 =====
        vo.setActiveProjectCount(nvl(reportMapper.countActiveProjects()));
        vo.setOnlineDeviceCount(nvl(reportMapper.countOnlineDevices()));
        vo.setActiveEngineerCount(nvl(reportMapper.countActiveEngineers()));
        vo.setActiveAgentCount(nvl(reportMapper.countActiveAgentCompanies()));

        // ===== 上月环比基准（本月1号 = 上月底次日，即上月所有数据 create_time < 本月1号） =====
        LocalDate firstOfThisMonth = YearMonth.now().atDay(1);
        Long lastProject = nvl(reportMapper.countProjectsBefore(firstOfThisMonth));
        Long lastDevice = nvl(reportMapper.countDevicesBefore(firstOfThisMonth));
        Long lastEngineer = nvl(reportMapper.countEngineersBefore(firstOfThisMonth));
        Long lastAgent = nvl(reportMapper.countAgentCompaniesBefore(firstOfThisMonth));

        vo.setLastMonthProjectCount(lastProject);
        vo.setLastMonthDeviceCount(lastDevice);
        vo.setLastMonthEngineerCount(lastEngineer);
        vo.setLastMonthAgentCount(lastAgent);

        // ===== 环比增长率 =====
        vo.setProjectGrowthRate(growthRate(vo.getProjectCount(), lastProject));
        vo.setDeviceGrowthRate(growthRate(vo.getDeviceCount(), lastDevice));
        vo.setEngineerGrowthRate(growthRate(vo.getEngineerCount(), lastEngineer));
        vo.setAgentGrowthRate(growthRate(vo.getAgentCompanyCount(), lastAgent));

        return vo;
    }

    @Override
    public List<ChartDataVO> getProjectPhaseDistribution() {
        return reportMapper.countProjectsByPhase();
    }

    @Override
    public List<ChartDataVO> getProjectTrend() {
        // 近12月趋势：取当年数据（若跨年需前端按月份对齐，此处按当前年份返回）
        int year = LocalDate.now().getYear();
        return reportMapper.countMonthlyProjects(year);
    }

    @Override
    public List<RiskProjectVO> getRiskProjects() {
        return reportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE);
    }

    /* ============ 私有工具 ============ */

    /**
     * 计算环比增长率（百分比）。
     * <p>公式：(current - previous) / previous * 100，保留 1 位小数。</p>
     * <p>previous 为 0 时：current > 0 记 100.0，否则记 0.0。</p>
     */
    private Double growthRate(Long current, Long previous) {
        if (previous == null || previous == 0L) {
            return (current != null && current > 0L) ? 100.0 : 0.0;
        }
        long cur = Objects.requireNonNullElse(current, 0L);
        double rate = (cur - previous) * 100.0 / previous;
        return Math.round(rate * 10.0) / 10.0;
    }

    /** Long 空值兜底为 0 */
    private Long nvl(Long v) {
        return v == null ? 0L : v;
    }
}
