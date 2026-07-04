package com.vibe.report.service.impl;

import com.vibe.report.constant.ReportConstant;
import com.vibe.report.mapper.ReportMapper;
import com.vibe.report.service.ManagementCockpitService;
import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.CockpitAggregatedVO;
import com.vibe.report.vo.CockpitKpiVO;
import com.vibe.report.vo.CockpitStatVO;
import com.vibe.report.vo.PhaseDistributionVO;
import com.vibe.report.vo.ProjectTrendVO;
import com.vibe.report.vo.RiskProjectVO;
import com.vibe.report.vo.RiskWarningVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
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

    @Override
    public CockpitAggregatedVO getAggregated() {
        CockpitAggregatedVO vo = new CockpitAggregatedVO();
        vo.setKpi(buildKpi());
        vo.setPhaseDistribution(buildPhaseDistribution());
        vo.setProjectTrend(buildProjectTrend());
        vo.setRiskWarnings(buildRiskWarnings());
        // todoList 和 recentActivities 默认空列表（后续接入审批/操作日志模块）
        return vo;
    }

    /* ============ 聚合数据转换 ============ */

    /**
     * 构建 KPI（对齐前端 CockpitKpi 字段名）
     *
     * <p>当前数据源：在建项目数来自 countActiveProjects，
     * 风险项目数来自 selectRiskProjects 列表大小，
     * 其余指标暂为 0（后续接入专项统计 SQL）。</p>
     */
    private CockpitKpiVO buildKpi() {
        CockpitKpiVO kpi = new CockpitKpiVO();
        kpi.setOngoingProjects(nvl(reportMapper.countActiveProjects()));
        List<RiskProjectVO> risks = reportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE);
        kpi.setRiskProjects((long) risks.size());
        // 超期项目数 = 风险项目中 riskType 为 PROJECT_OVERDUE 的数量
        kpi.setOverdueProjects(risks.stream()
                .filter(r -> "PROJECT_OVERDUE".equals(r.getRiskType()))
                .count());
        // 本月新增/结项暂用趋势首月数据兜底（后续接入按月统计 SQL）
        kpi.setMonthNewProjects(0L);
        kpi.setMonthClosedProjects(0L);
        // 利用率/到货率/验收率暂为 0（后续接入专项计算 SQL）
        kpi.setEngineerUtilization(0.0);
        kpi.setDeviceArrivalRate(0.0);
        kpi.setAcceptanceCompletionRate(0.0);
        return kpi;
    }

    /**
     * 构建项目阶段分布（ChartDataVO → PhaseDistributionVO）
     *
     * <p>后端 ChartDataVO 使用 name/value，前端 PhaseDistribution 使用 phase/phaseName/count。</p>
     */
    private List<PhaseDistributionVO> buildPhaseDistribution() {
        List<ChartDataVO> charts = reportMapper.countProjectsByPhase();
        List<PhaseDistributionVO> result = new ArrayList<>(charts.size());
        for (ChartDataVO c : charts) {
            result.add(new PhaseDistributionVO(c.getName(), c.getName(), nvl(c.getValue())));
        }
        return result;
    }

    /**
     * 构建项目趋势（ChartDataVO → ProjectTrendVO）
     *
     * <p>后端 ChartDataVO 使用 completedCount，前端 ProjectTrend 使用 closedCount。</p>
     */
    private List<ProjectTrendVO> buildProjectTrend() {
        int year = LocalDate.now().getYear();
        List<ChartDataVO> charts = reportMapper.countMonthlyProjects(year);
        List<ProjectTrendVO> result = new ArrayList<>(charts.size());
        for (ChartDataVO c : charts) {
            result.add(new ProjectTrendVO(
                    c.getMonth(),
                    nvl(c.getNewCount()),
                    nvl(c.getCompletedCount()),
                    0L  // ongoingCount 暂为 0，后续接入累计在建计算
            ));
        }
        return result;
    }

    /**
     * 构建风险预警（RiskProjectVO → RiskWarningVO）
     *
     * <p>对齐前端 RiskWarning 字段名，并补充 level/riskType 映射。</p>
     */
    private List<RiskWarningVO> buildRiskWarnings() {
        List<RiskProjectVO> risks = reportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE);
        List<RiskWarningVO> result = new ArrayList<>(risks.size());
        for (RiskProjectVO r : risks) {
            RiskWarningVO w = new RiskWarningVO();
            w.setId(r.getProjectId());  // 使用项目 ID 作为风险项 ID
            w.setProjectId(r.getProjectId());
            w.setProjectName(r.getProjectName());
            w.setRiskType(mapRiskType(r.getRiskType()));
            w.setDescription(r.getDescription());
            w.setLevel(mapRiskLevel(r.getRiskType()));
            w.setDetectedAt(r.getPlannedEnd() != null ? r.getPlannedEnd() : LocalDate.now());
            result.add(w);
        }
        return result;
    }

    /** 后端风险类型 → 前端期望的 PROGRESS/DEVICE/RESOURCE/AGENT/OTHER */
    private String mapRiskType(String backendRiskType) {
        if (backendRiskType == null) return "OTHER";
        return switch (backendRiskType) {
            case "PROGRESS_DELAY", "PROJECT_OVERDUE", "OVERDUE_TASK" -> "PROGRESS";
            case "UNRESOLVED_ISSUE" -> "OTHER";
            default -> "OTHER";
        };
    }

    /** 根据风险类型推断等级（PROJECT_OVERDUE → HIGH，PROGRESS_DELAY → MEDIUM，其他 → MEDIUM） */
    private String mapRiskLevel(String backendRiskType) {
        if (backendRiskType == null) return "MEDIUM";
        return switch (backendRiskType) {
            case "PROJECT_OVERDUE" -> "HIGH";
            case "PROGRESS_DELAY", "OVERDUE_TASK" -> "MEDIUM";
            default -> "MEDIUM";
        };
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
