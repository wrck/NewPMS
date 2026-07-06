package com.vibe.report.service.impl;

import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.report.constant.ReportConstant;
import com.vibe.report.mapper.FinanceReportMapper;
import com.vibe.report.mapper.ProjectReportMapper;
import com.vibe.report.mapper.ResourceReportMapper;
import com.vibe.report.service.DashboardService;
import com.vibe.report.service.ManagementCockpitService;
import com.vibe.report.vo.AgentDashboardVO;
import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.DashboardVO;
import com.vibe.report.vo.DeliverableItemVO;
import com.vibe.report.vo.DirectorDashboardVO;
import com.vibe.report.vo.EngineerDashboardVO;
import com.vibe.report.vo.OutsourceTaskItemVO;
import com.vibe.report.vo.PmDashboardVO;
import com.vibe.report.vo.ProjectItemVO;
import com.vibe.report.vo.RiskProjectVO;
import com.vibe.report.vo.TaskItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

/**
 * 工作台首页服务实现
 *
 * <p>通过 {@link UserContextHolder} 获取当前用户角色，按角色优先级返回差异化首页数据。
 * 角色判定优先级：SUPER_ADMIN > DIRECTOR > PM > ENGINEER > AGENT_ADMIN > AGENT_ENGINEER。</p>
 *
 * <p>数据权限：</p>
 * <ul>
 *   <li>PM：仅看自己负责的项目（project.pm_id = userId）</li>
 *   <li>ENGINEER：仅看自己任务（project_task.assignee_id → engineer.user_id = userId）</li>
 *   <li>AGENT：仅看本公司转包任务（outsource_task.agent_company_id = tenantId）</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProjectReportMapper projectReportMapper;
    private final ResourceReportMapper resourceReportMapper;
    private final FinanceReportMapper financeReportMapper;
    private final ManagementCockpitService managementCockpitService;

    @Override
    public DashboardVO getDashboard() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }

        DashboardVO vo = new DashboardVO();
        vo.setRealName(ctx.getRealName());

        // 按角色优先级判定
        String role = resolvePrimaryRole(ctx);
        vo.setRole(role);

        switch (role) {
            case ReportConstant.ROLE_SUPER_ADMIN:
            case ReportConstant.ROLE_DIRECTOR:
                vo.setDirector(buildDirectorDashboard());
                break;
            case ReportConstant.ROLE_PM:
                vo.setPm(buildPmDashboard(ctx.getUserId()));
                break;
            case ReportConstant.ROLE_ENGINEER:
                vo.setEngineer(buildEngineerDashboard(ctx.getUserId()));
                break;
            case ReportConstant.ROLE_AGENT_ADMIN:
            case ReportConstant.ROLE_AGENT_ENGINEER:
                vo.setAgent(buildAgentDashboard(ctx.getTenantId()));
                break;
            default:
                // 无明确业务角色，默认走总监视图（含全局只读数据）
                vo.setDirector(buildDirectorDashboard());
        }
        return vo;
    }

    /* ============ 总监首页 ============ */

    private DirectorDashboardVO buildDirectorDashboard() {
        DirectorDashboardVO vo = new DirectorDashboardVO();
        // 核心指标（含环比）
        vo.setStats(managementCockpitService.getStats());
        // 审批待办
        Long pendingChange = nvl(projectReportMapper.countPendingChangeLogs());
        Long pendingWorkload = nvl(financeReportMapper.countPendingWorkloads());
        vo.setPendingChangeCount(pendingChange);
        vo.setPendingWorkloadCount(pendingWorkload);
        vo.setPendingApprovalCount(pendingChange + pendingWorkload);
        // 项目状态分布（饼图）
        vo.setProjectStatusDist(projectReportMapper.countProjectsByStatus());
        // 近12月项目趋势（折线图）
        vo.setProjectTrend(managementCockpitService.getProjectTrend());
        // 风险项目
        List<RiskProjectVO> risks = projectReportMapper.selectRiskProjects(ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setRiskProjects(risks != null ? risks : Collections.emptyList());
        return vo;
    }

    /* ============ PM 首页 ============ */

    private PmDashboardVO buildPmDashboard(Long userId) {
        PmDashboardVO vo = new PmDashboardVO();
        vo.setMyProjectCount(nvl(projectReportMapper.countProjectsByPm(userId)));
        vo.setActiveProjectCount(nvl(projectReportMapper.countActiveProjectsByPm(userId)));
        vo.setPendingDispatchCount(nvl(projectReportMapper.countPendingDispatchByPm(userId)));
        vo.setPendingReviewCount(nvl(projectReportMapper.countPendingReviewByPm(userId)));
        // 风险项目数（取风险列表中属于该 PM 的数量，此处简化为全局风险数）
        List<RiskProjectVO> allRisks = projectReportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE);
        vo.setRiskProjectCount(allRisks != null ? (long) allRisks.size() : 0L);
        // 我的项目列表
        List<ProjectItemVO> projects = projectReportMapper.selectMyProjects(userId, ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setMyProjects(projects != null ? projects : Collections.emptyList());
        // 待派单任务（保留原行为：按 engineer.user_id 查询，PM 兼任工程师场景下返回其待处理任务）
        List<TaskItemVO> tasks = resourceReportMapper.selectMyTasks(userId, ReportConstant.TASK_PENDING,
                ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setPendingDispatchTasks(tasks != null ? tasks : Collections.emptyList());
        // 待审核交付物
        List<DeliverableItemVO> deliverables = projectReportMapper.selectMyDeliverables(userId,
                ReportConstant.OUTSOURCE_SUBMITTED, ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setPendingReviewDeliverables(deliverables != null ? deliverables : Collections.emptyList());
        return vo;
    }

    /* ============ 工程师首页 ============ */

    private EngineerDashboardVO buildEngineerDashboard(Long userId) {
        EngineerDashboardVO vo = new EngineerDashboardVO();
        LocalDate today = LocalDate.now();

        vo.setTodayTaskCount(nvl(resourceReportMapper.countTodayTasksByEngineer(userId, today)));
        vo.setPendingTaskCount(nvl(resourceReportMapper.countPendingTasksByEngineer(userId)));
        vo.setOverdueTaskCount(nvl(resourceReportMapper.countOverdueTasksByEngineer(userId, today)));

        // 工时统计：今日 / 本周（周一至周日） / 本月
        BigDecimal todayHours = resourceReportMapper.sumWorkHours(userId, today, today);
        vo.setTodayWorkHours(todayHours != null ? todayHours : BigDecimal.ZERO);

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        BigDecimal weekHours = resourceReportMapper.sumWorkHours(userId, weekStart, weekEnd);
        vo.setWeekWorkHours(weekHours != null ? weekHours : BigDecimal.ZERO);

        LocalDate monthStart = YearMonth.now().atDay(1);
        LocalDate monthEnd = YearMonth.now().atEndOfMonth();
        BigDecimal monthHours = resourceReportMapper.sumWorkHours(userId, monthStart, monthEnd);
        vo.setMonthWorkHours(monthHours != null ? monthHours : BigDecimal.ZERO);

        // 今日任务列表（计划区间覆盖今天的未完成任务）
        List<TaskItemVO> todayTasks = resourceReportMapper.selectMyTasks(userId, null, ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setTodayTasks(todayTasks != null ? todayTasks : Collections.emptyList());

        // 超期任务列表
        List<TaskItemVO> overdueTasks = resourceReportMapper.selectOverdueTasks(userId, today, ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setOverdueTasks(overdueTasks != null ? overdueTasks : Collections.emptyList());
        return vo;
    }

    /* ============ 代理商首页 ============ */

    private AgentDashboardVO buildAgentDashboard(Long agentCompanyId) {
        AgentDashboardVO vo = new AgentDashboardVO();
        if (agentCompanyId == null) {
            // 代理商租户ID缺失，返回空数据
            vo.setTotalCount(0L);
            vo.setPendingCount(0L);
            vo.setInProgressCount(0L);
            vo.setSubmittedCount(0L);
            vo.setOverdueCount(0L);
            vo.setPendingTasks(Collections.emptyList());
            vo.setInProgressTasks(Collections.emptyList());
            vo.setSubmittedTasks(Collections.emptyList());
            return vo;
        }

        vo.setTotalCount(nvl(financeReportMapper.countAgentTasks(agentCompanyId, null)));
        vo.setPendingCount(nvl(financeReportMapper.countAgentTasks(agentCompanyId, ReportConstant.OUTSOURCE_PENDING)));
        vo.setInProgressCount(nvl(financeReportMapper.countAgentTasks(agentCompanyId, ReportConstant.OUTSOURCE_IN_PROGRESS)));
        vo.setSubmittedCount(nvl(financeReportMapper.countAgentTasks(agentCompanyId, ReportConstant.OUTSOURCE_SUBMITTED)));
        // 超期数
        List<OutsourceTaskItemVO> allTasks = financeReportMapper.selectAgentTasks(agentCompanyId, null, ReportConstant.RISK_LIST_SIZE);
        long overdue = 0L;
        if (allTasks != null) {
            overdue = allTasks.stream().filter(t -> t.getOverdue() != null && t.getOverdue() == 1).count();
        }
        vo.setOverdueCount(overdue);

        // 各状态任务列表
        List<OutsourceTaskItemVO> pending = financeReportMapper.selectAgentTasks(agentCompanyId,
                ReportConstant.OUTSOURCE_PENDING, ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setPendingTasks(pending != null ? pending : Collections.emptyList());

        List<OutsourceTaskItemVO> inProgress = financeReportMapper.selectAgentTasks(agentCompanyId,
                ReportConstant.OUTSOURCE_IN_PROGRESS, ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setInProgressTasks(inProgress != null ? inProgress : Collections.emptyList());

        List<OutsourceTaskItemVO> submitted = financeReportMapper.selectAgentTasks(agentCompanyId,
                ReportConstant.OUTSOURCE_SUBMITTED, ReportConstant.DASHBOARD_LIST_SIZE);
        vo.setSubmittedTasks(submitted != null ? submitted : Collections.emptyList());
        return vo;
    }

    /* ============ 私有工具 ============ */

    /**
     * 解析当前用户主角色（按业务优先级）。
     * 优先级：SUPER_ADMIN > DIRECTOR > PM > ENGINEER > AGENT_ADMIN > AGENT_ENGINEER
     */
    private String resolvePrimaryRole(UserContext ctx) {
        if (ctx.hasRole(ReportConstant.ROLE_SUPER_ADMIN)) {
            return ReportConstant.ROLE_SUPER_ADMIN;
        }
        if (ctx.hasRole(ReportConstant.ROLE_DIRECTOR)) {
            return ReportConstant.ROLE_DIRECTOR;
        }
        if (ctx.hasRole(ReportConstant.ROLE_PM)) {
            return ReportConstant.ROLE_PM;
        }
        if (ctx.hasRole(ReportConstant.ROLE_ENGINEER)) {
            return ReportConstant.ROLE_ENGINEER;
        }
        if (ctx.hasRole(ReportConstant.ROLE_AGENT_ADMIN)) {
            return ReportConstant.ROLE_AGENT_ADMIN;
        }
        if (ctx.hasRole(ReportConstant.ROLE_AGENT_ENGINEER)) {
            return ReportConstant.ROLE_AGENT_ENGINEER;
        }
        // 兜底：代理商租户类型走代理商视图，其余走总监视图
        if (ctx.isAgent()) {
            return ReportConstant.ROLE_AGENT_ADMIN;
        }
        return ReportConstant.ROLE_DIRECTOR;
    }

    /** Long 空值兜底为 0 */
    private Long nvl(Long v) {
        return v == null ? 0L : v;
    }
}
