package com.vibe.report.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.es.ElasticSearchService;
import com.vibe.es.index.EsIndexConstant;
import com.vibe.report.constant.ReportConstant;
import com.vibe.report.mapper.DeviceReportMapper;
import com.vibe.report.mapper.FinanceReportMapper;
import com.vibe.report.mapper.ProjectReportMapper;
import com.vibe.report.mapper.ResourceReportMapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
 * <p>高并发聚合查询走 ES（{@code vibe.es.enabled=true} 时启用）：
 * 项目阶段分布、项目月度趋势通过 ES terms / date_histogram 聚合加速，避免 MySQL 全表扫描；
 * ES 不可用或解析失败时兜底回 MySQL。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagementCockpitServiceImpl implements ManagementCockpitService {

    private final ProjectReportMapper projectReportMapper;
    private final DeviceReportMapper deviceReportMapper;
    private final ResourceReportMapper resourceReportMapper;
    private final FinanceReportMapper financeReportMapper;
    private final ElasticSearchService<?> elasticSearchService;

    /** Jackson ObjectMapper（ES 聚合响应解析，线程安全） */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 是否启用 ES 聚合查询（高并发场景走 ES，默认 false 兜底 MySQL）。
     * 通过 {@code vibe.es.enabled} 配置注入。
     */
    @Value("${vibe.es.enabled:false}")
    private boolean esEnabled;

    @Override
    public CockpitStatVO getStats() {
        CockpitStatVO vo = new CockpitStatVO();

        // ===== 当期总数 =====
        vo.setProjectCount(nvl(projectReportMapper.countAllProjects()));
        vo.setDeviceCount(nvl(deviceReportMapper.countAllDevices()));
        vo.setEngineerCount(nvl(resourceReportMapper.countAllEngineers()));
        vo.setAgentCompanyCount(nvl(financeReportMapper.countAllAgentCompanies()));

        // ===== 活跃数 =====
        vo.setActiveProjectCount(nvl(projectReportMapper.countActiveProjects()));
        vo.setOnlineDeviceCount(nvl(deviceReportMapper.countOnlineDevices()));
        vo.setActiveEngineerCount(nvl(resourceReportMapper.countActiveEngineers()));
        vo.setActiveAgentCount(nvl(financeReportMapper.countActiveAgentCompanies()));

        // ===== 上月环比基准（本月1号 = 上月底次日，即上月所有数据 create_time < 本月1号） =====
        LocalDate firstOfThisMonth = YearMonth.now().atDay(1);
        Long lastProject = nvl(projectReportMapper.countProjectsBefore(firstOfThisMonth));
        Long lastDevice = nvl(deviceReportMapper.countDevicesBefore(firstOfThisMonth));
        Long lastEngineer = nvl(resourceReportMapper.countEngineersBefore(firstOfThisMonth));
        Long lastAgent = nvl(financeReportMapper.countAgentCompaniesBefore(firstOfThisMonth));

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
    @Cacheable(cacheNames = "dashboardStats",
            key = "'cockpit:phaseDist'")
    public List<ChartDataVO> getProjectPhaseDistribution() {
        // 高并发场景走 ES 聚合（terms on phase field），失败兜底 MySQL
        if (esEnabled) {
            List<ChartDataVO> esResult = aggregateTermsFromEs(
                    EsIndexConstant.INDEX_VIBE_PROJECT, "phase", "by_phase");
            if (esResult != null) {
                return esResult;
            }
        }
        return projectReportMapper.countProjectsByPhase();
    }

    @Override
    @Cacheable(cacheNames = "dashboardStats",
            key = "'cockpit:projectTrend:' + T(java.time.LocalDate).now().getYear()")
    public List<ChartDataVO> getProjectTrend() {
        // 近12月趋势：取当年数据（若跨年需前端按月份对齐，此处按当前年份返回）
        int year = LocalDate.now().getYear();
        // 高并发场景走 ES date_histogram 聚合，失败兜底 MySQL
        if (esEnabled) {
            List<ChartDataVO> esResult = aggregateMonthlyTrendFromEs(year);
            if (esResult != null) {
                return esResult;
            }
        }
        return projectReportMapper.countMonthlyProjects(year);
    }

    @Override
    public List<RiskProjectVO> getRiskProjects() {
        return projectReportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE);
    }

    @Override
    @Cacheable(cacheNames = "dashboardStats", key = "'cockpit:aggregated'")
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
        kpi.setOngoingProjects(nvl(projectReportMapper.countActiveProjects()));
        List<RiskProjectVO> risks = projectReportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE);
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
        List<ChartDataVO> charts = getProjectPhaseDistribution();
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
        List<ChartDataVO> charts = getProjectTrend();
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
        List<RiskProjectVO> risks = projectReportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE);
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

    /* ============ ES 聚合查询辅助 ============ */

    /**
     * ES terms 聚合 → {@link ChartDataVO} 列表。
     *
     * <p>调用 ES terms 聚合（按 {@code field} 字段分桶），将 buckets 转换为
     * ChartDataVO(name=key, value=doc_count)。失败时返回 null，由调用方兜底 MySQL。</p>
     *
     * @param indexName ES 索引名（参考 {@link EsIndexConstant}）
     * @param field     聚合字段名（ES 文档字段，如 status/phase）
     * @param aggName   聚合名称（任意，需与 DSL 中一致）
     * @return 聚合结果列表，失败返回 null
     */
    private List<ChartDataVO> aggregateTermsFromEs(String indexName, String field, String aggName) {
        String dsl = String.format(
                "{\"size\":0,\"aggs\":{\"%s\":{\"terms\":{\"field\":\"%s\",\"size\":50}}}}",
                aggName, field);
        try {
            String response = elasticSearchService.aggregate(indexName, dsl);
            if (response == null || response.isBlank() || "{}".equals(response)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode buckets = root.path("aggregations").path(aggName).path("buckets");
            if (!buckets.isArray() || buckets.isEmpty()) {
                return null;
            }
            List<ChartDataVO> result = new ArrayList<>(buckets.size());
            for (JsonNode bucket : buckets) {
                String key = bucket.path("key").asText("UNKNOWN");
                long count = bucket.path("doc_count").asLong(0L);
                result.add(new ChartDataVO(key, count));
            }
            return result;
        } catch (Exception e) {
            log.warn("ES terms 聚合失败，回退 MySQL：index={}, field={}, error={}",
                    indexName, field, e.getMessage());
            return null;
        }
    }

    /**
     * ES date_histogram 聚合 → {@link ChartDataVO} 列表（项目月度趋势）。
     *
     * <p>按 {@code createdAt} 字段做 calendar_interval=month 聚合，将 buckets 转换为
     * ChartDataVO(month=yyyy-MM, newCount=doc_count, completedCount=子聚合 status=CLOSE/ARCHIVED 之和)。
     * 失败时返回 null，由调用方兜底 MySQL。</p>
     *
     * @param year 趋势年份（用于过滤范围）
     * @return 趋势数据列表，失败返回 null
     */
    private List<ChartDataVO> aggregateMonthlyTrendFromEs(int year) {
        // 使用 date_histogram 按月分桶，子聚合 status terms 用于计算完成数
        String dsl = String.format(
                "{\"size\":0,\"query\":{\"range\":{\"createdAt\":{\"gte\":\"%d-01-01\",\"lt\":\"%d-01-01\"}}}," +
                "\"aggs\":{\"monthly\":{\"date_histogram\":{\"field\":\"createdAt\",\"calendar_interval\":\"month\"," +
                "\"format\":\"yyyy-MM\"},\"aggs\":{\"status\":{\"terms\":{\"field\":\"status\"}}}}}}",
                year, year + 1);
        try {
            String response = elasticSearchService.aggregate(EsIndexConstant.INDEX_VIBE_PROJECT, dsl);
            if (response == null || response.isBlank() || "{}".equals(response)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode buckets = root.path("aggregations").path("monthly").path("buckets");
            if (!buckets.isArray() || buckets.isEmpty()) {
                return null;
            }
            List<ChartDataVO> result = new ArrayList<>(buckets.size());
            for (JsonNode bucket : buckets) {
                String month = bucket.path("key_as_string").asText(bucket.path("key").asText(""));
                long newCount = bucket.path("doc_count").asLong(0L);
                long completedCount = 0L;
                JsonNode statusBuckets = bucket.path("status").path("buckets");
                if (statusBuckets.isArray()) {
                    for (JsonNode sb : statusBuckets) {
                        String status = sb.path("key").asText("");
                        if ("CLOSE".equals(status) || "ARCHIVED".equals(status)) {
                            completedCount += sb.path("doc_count").asLong(0L);
                        }
                    }
                }
                result.add(new ChartDataVO(month, newCount, completedCount));
            }
            return result;
        } catch (Exception e) {
            log.warn("ES date_histogram 聚合失败，回退 MySQL：index={}, year={}, error={}",
                    EsIndexConstant.INDEX_VIBE_PROJECT, year, e.getMessage());
            return null;
        }
    }

    /* ============ 私有工具 ============ */

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
