package com.vibe.report.service.impl;

import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.report.constant.ReportConstant;
import com.vibe.report.mapper.FinanceReportMapper;
import com.vibe.report.mapper.ProjectReportMapper;
import com.vibe.report.mapper.ResourceReportMapper;
import com.vibe.report.service.ManagementCockpitService;
import com.vibe.report.vo.AgentDashboardVO;
import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.CockpitStatVO;
import com.vibe.report.vo.DashboardVO;
import com.vibe.report.vo.DirectorDashboardVO;
import com.vibe.report.vo.EngineerDashboardVO;
import com.vibe.report.vo.OutsourceTaskItemVO;
import com.vibe.report.vo.PmDashboardVO;
import com.vibe.report.vo.RiskProjectVO;
import com.vibe.report.vo.TaskItemVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作台首页服务实现单元测试（Task 3 SubTask 3.6）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>未登录拦截：UserContext 为空或 userId 为空抛 UNAUTHORIZED</li>
 *   <li>角色差异化：SUPER_ADMIN/DIRECTOR → 总监视图，PM → PM 视图，ENGINEER → 工程师视图，
 *       AGENT_ADMIN/AGENT_ENGINEER → 代理商视图，无角色 → 默认总监</li>
 *   <li>总监视图：审批待办聚合、风险项目兜底空列表</li>
 *   <li>PM 视图：6 项计数与 3 个列表（含 null 兜底）</li>
 *   <li>工程师视图：今日/本周/本月工时计算、null 工时兜底 0</li>
 *   <li>代理商视图：4 类任务计数、超期数过滤、tenantId 缺失返回空数据</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("工作台首页 DashboardServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ProjectReportMapper projectReportMapper;
    @Mock
    private ResourceReportMapper resourceReportMapper;
    @Mock
    private FinanceReportMapper financeReportMapper;
    @Mock
    private ManagementCockpitService managementCockpitService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /* ============ 未登录拦截 ============ */

    @Nested
    @DisplayName("未登录拦截")
    class AuthTest {

        @Test
        @DisplayName("UserContext 为空抛 UNAUTHORIZED")
        void should_throw_unauthorized_when_context_null() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> dashboardService.getDashboard());

            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("userId 为空抛 UNAUTHORIZED")
        void should_throw_unauthorized_when_user_id_null() {
            UserContextHolder.set(UserContext.builder().userId(null).build());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> dashboardService.getDashboard());

            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    /* ============ 总监视图 ============ */

    @Nested
    @DisplayName("总监/SUPER_ADMIN/默认 视图")
    class DirectorDashboardTest {

        @Test
        @DisplayName("DIRECTOR 角色填充 director 视图，其余子视图为 null")
        void should_build_director_dashboard_for_director_role() {
            UserContextHolder.set(buildCtx(1L, "张总监",
                    List.of(ReportConstant.ROLE_DIRECTOR)));
            stubDirectorMappers();

            DashboardVO vo = dashboardService.getDashboard();

            assertNotNull(vo);
            assertEquals(ReportConstant.ROLE_DIRECTOR, vo.getRole());
            assertEquals("张总监", vo.getRealName());
            assertNotNull(vo.getDirector());
            assertNull(vo.getPm());
            assertNull(vo.getEngineer());
            assertNull(vo.getAgent());
        }

        @Test
        @DisplayName("SUPER_ADMIN 角色优先级最高，走总监视图")
        void should_build_director_dashboard_for_super_admin_role() {
            UserContextHolder.set(UserContext.builder()
                    .userId(1L).realName("超管")
                    .roles(List.of(ReportConstant.ROLE_PM, ReportConstant.ROLE_SUPER_ADMIN))
                    .build());
            stubDirectorMappers();

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(ReportConstant.ROLE_SUPER_ADMIN, vo.getRole());
            assertNotNull(vo.getDirector());
            assertNull(vo.getPm());
        }

        @Test
        @DisplayName("无任何业务角色时默认走总监视图")
        void should_default_to_director_when_no_role_matches() {
            UserContextHolder.set(buildCtx(5L, "新员工",
                    Collections.emptyList()));
            stubDirectorMappers();

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(ReportConstant.ROLE_DIRECTOR, vo.getRole());
            assertNotNull(vo.getDirector());
        }

        @Test
        @DisplayName("pendingApprovalCount = pendingChangeCount + pendingWorkloadCount")
        void should_aggregate_pending_approval_count() {
            UserContextHolder.set(buildCtx(1L, "总监", List.of(ReportConstant.ROLE_DIRECTOR)));
            when(managementCockpitService.getStats()).thenReturn(new CockpitStatVO());
            when(projectReportMapper.countPendingChangeLogs()).thenReturn(3L);
            when(financeReportMapper.countPendingWorkloads()).thenReturn(7L);
            when(projectReportMapper.countProjectsByStatus()).thenReturn(Collections.emptyList());
            when(managementCockpitService.getProjectTrend()).thenReturn(Collections.emptyList());
            when(projectReportMapper.selectRiskProjects(anyInt())).thenReturn(Collections.emptyList());

            DashboardVO vo = dashboardService.getDashboard();

            DirectorDashboardVO d = vo.getDirector();
            assertAll("审批待办聚合",
                    () -> assertEquals(3L, d.getPendingChangeCount()),
                    () -> assertEquals(7L, d.getPendingWorkloadCount()),
                    () -> assertEquals(10L, d.getPendingApprovalCount())
            );
        }

        @Test
        @DisplayName("selectRiskProjects 返回 null 时 riskProjects 兜底为空列表")
        void should_fallback_to_empty_list_when_risk_projects_null() {
            UserContextHolder.set(buildCtx(1L, "总监", List.of(ReportConstant.ROLE_DIRECTOR)));
            when(managementCockpitService.getStats()).thenReturn(new CockpitStatVO());
            when(projectReportMapper.countPendingChangeLogs()).thenReturn(0L);
            when(financeReportMapper.countPendingWorkloads()).thenReturn(0L);
            when(projectReportMapper.countProjectsByStatus()).thenReturn(Collections.emptyList());
            when(managementCockpitService.getProjectTrend()).thenReturn(Collections.emptyList());
            when(projectReportMapper.selectRiskProjects(ReportConstant.DASHBOARD_LIST_SIZE)).thenReturn(null);

            DashboardVO vo = dashboardService.getDashboard();

            assertNotNull(vo.getDirector().getRiskProjects());
            assertEquals(0, vo.getDirector().getRiskProjects().size());
        }

        private void stubDirectorMappers() {
            when(managementCockpitService.getStats()).thenReturn(new CockpitStatVO());
            when(projectReportMapper.countPendingChangeLogs()).thenReturn(0L);
            when(financeReportMapper.countPendingWorkloads()).thenReturn(0L);
            when(projectReportMapper.countProjectsByStatus()).thenReturn(Collections.emptyList());
            when(managementCockpitService.getProjectTrend()).thenReturn(Collections.emptyList());
            when(projectReportMapper.selectRiskProjects(ReportConstant.DASHBOARD_LIST_SIZE))
                    .thenReturn(Collections.emptyList());
        }
    }

    /* ============ PM 视图 ============ */

    @Nested
    @DisplayName("PM 视图")
    class PmDashboardTest {

        @Test
        @DisplayName("PM 角色填充 pm 视图：6 项计数 + 3 个列表")
        void should_build_pm_dashboard_with_project_counts() {
            UserContextHolder.set(buildCtx(20L, "李PM", List.of(ReportConstant.ROLE_PM)));
            when(projectReportMapper.countProjectsByPm(20L)).thenReturn(5L);
            when(projectReportMapper.countActiveProjectsByPm(20L)).thenReturn(3L);
            when(projectReportMapper.countPendingDispatchByPm(20L)).thenReturn(2L);
            when(projectReportMapper.countPendingReviewByPm(20L)).thenReturn(1L);
            when(projectReportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE))
                    .thenReturn(List.of(buildRisk(1L)));
            when(projectReportMapper.selectMyProjects(eq(20L), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(resourceReportMapper.selectMyTasks(eq(20L), eq(ReportConstant.TASK_PENDING), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.selectMyDeliverables(eq(20L), eq(ReportConstant.OUTSOURCE_SUBMITTED), anyInt()))
                    .thenReturn(Collections.emptyList());

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(ReportConstant.ROLE_PM, vo.getRole());
            PmDashboardVO pm = vo.getPm();
            assertNotNull(pm);
            assertAll("PM 计数",
                    () -> assertEquals(5L, pm.getMyProjectCount()),
                    () -> assertEquals(3L, pm.getActiveProjectCount()),
                    () -> assertEquals(2L, pm.getPendingDispatchCount()),
                    () -> assertEquals(1L, pm.getPendingReviewCount()),
                    () -> assertEquals(1L, pm.getRiskProjectCount())
            );
            assertAll("列表非空",
                    () -> assertNotNull(pm.getMyProjects()),
                    () -> assertNotNull(pm.getPendingDispatchTasks()),
                    () -> assertNotNull(pm.getPendingReviewDeliverables())
            );
            assertNull(vo.getDirector());
            assertNull(vo.getEngineer());
            assertNull(vo.getAgent());
        }

        @Test
        @DisplayName("selectRiskProjects 返回 null 时 riskProjectCount = 0")
        void should_handle_null_risk_list() {
            UserContextHolder.set(buildCtx(20L, "PM", List.of(ReportConstant.ROLE_PM)));
            when(projectReportMapper.countProjectsByPm(20L)).thenReturn(0L);
            when(projectReportMapper.countActiveProjectsByPm(20L)).thenReturn(0L);
            when(projectReportMapper.countPendingDispatchByPm(20L)).thenReturn(0L);
            when(projectReportMapper.countPendingReviewByPm(20L)).thenReturn(0L);
            when(projectReportMapper.selectRiskProjects(ReportConstant.RISK_LIST_SIZE)).thenReturn(null);
            when(projectReportMapper.selectMyProjects(eq(20L), anyInt())).thenReturn(Collections.emptyList());
            when(resourceReportMapper.selectMyTasks(eq(20L), eq(ReportConstant.TASK_PENDING), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(projectReportMapper.selectMyDeliverables(eq(20L), eq(ReportConstant.OUTSOURCE_SUBMITTED), anyInt()))
                    .thenReturn(Collections.emptyList());

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(0L, vo.getPm().getRiskProjectCount());
            assertNotNull(vo.getPm().getMyProjects());
        }
    }

    /* ============ 工程师视图 ============ */

    @Nested
    @DisplayName("工程师视图")
    class EngineerDashboardTest {

        @Test
        @DisplayName("ENGINEER 角色填充工程师视图：今日/待处理/超期任务数 + 今日/本周/本月工时")
        void should_build_engineer_dashboard_with_work_hours() {
            UserContextHolder.set(buildCtx(30L, "王工", List.of(ReportConstant.ROLE_ENGINEER)));
            when(resourceReportMapper.countTodayTasksByEngineer(eq(30L), any(LocalDate.class))).thenReturn(2L);
            when(resourceReportMapper.countPendingTasksByEngineer(30L)).thenReturn(5L);
            when(resourceReportMapper.countOverdueTasksByEngineer(eq(30L), any(LocalDate.class))).thenReturn(1L);
            when(resourceReportMapper.sumWorkHours(eq(30L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("8.0"));
            when(resourceReportMapper.selectMyTasks(eq(30L), eq(null), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(resourceReportMapper.selectOverdueTasks(eq(30L), any(LocalDate.class), anyInt()))
                    .thenReturn(Collections.emptyList());

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(ReportConstant.ROLE_ENGINEER, vo.getRole());
            EngineerDashboardVO eng = vo.getEngineer();
            assertNotNull(eng);
            assertAll("任务计数",
                    () -> assertEquals(2L, eng.getTodayTaskCount()),
                    () -> assertEquals(5L, eng.getPendingTaskCount()),
                    () -> assertEquals(1L, eng.getOverdueTaskCount())
            );
            assertAll("工时统计",
                    () -> assertEquals(new BigDecimal("8.0"), eng.getTodayWorkHours()),
                    () -> assertEquals(new BigDecimal("8.0"), eng.getWeekWorkHours()),
                    () -> assertEquals(new BigDecimal("8.0"), eng.getMonthWorkHours())
            );
        }

        @Test
        @DisplayName("sumWorkHours 返回 null 时 todayWorkHours 兜底为 0")
        void should_handle_null_work_hours_as_zero() {
            UserContextHolder.set(buildCtx(30L, "工程师", List.of(ReportConstant.ROLE_ENGINEER)));
            when(resourceReportMapper.countTodayTasksByEngineer(eq(30L), any(LocalDate.class))).thenReturn(0L);
            when(resourceReportMapper.countPendingTasksByEngineer(30L)).thenReturn(0L);
            when(resourceReportMapper.countOverdueTasksByEngineer(eq(30L), any(LocalDate.class))).thenReturn(0L);
            when(resourceReportMapper.sumWorkHours(eq(30L), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(null);
            when(resourceReportMapper.selectMyTasks(eq(30L), eq(null), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(resourceReportMapper.selectOverdueTasks(eq(30L), any(LocalDate.class), anyInt()))
                    .thenReturn(Collections.emptyList());

            DashboardVO vo = dashboardService.getDashboard();

            EngineerDashboardVO eng = vo.getEngineer();
            assertEquals(BigDecimal.ZERO, eng.getTodayWorkHours());
            assertEquals(BigDecimal.ZERO, eng.getWeekWorkHours());
            assertEquals(BigDecimal.ZERO, eng.getMonthWorkHours());
        }
    }

    /* ============ 代理商视图 ============ */

    @Nested
    @DisplayName("代理商视图")
    class AgentDashboardTest {

        @Test
        @DisplayName("AGENT_ADMIN 角色填充代理商视图：4 类任务计数")
        void should_build_agent_dashboard_with_status_counts() {
            UserContext ctx = UserContext.builder()
                    .userId(40L).realName("代理商A").tenantType("AGENT").tenantId(99L)
                    .roles(List.of(ReportConstant.ROLE_AGENT_ADMIN))
                    .build();
            UserContextHolder.set(ctx);
            when(financeReportMapper.countAgentTasks(99L, null)).thenReturn(10L);
            when(financeReportMapper.countAgentTasks(99L, ReportConstant.OUTSOURCE_PENDING)).thenReturn(3L);
            when(financeReportMapper.countAgentTasks(99L, ReportConstant.OUTSOURCE_IN_PROGRESS)).thenReturn(4L);
            when(financeReportMapper.countAgentTasks(99L, ReportConstant.OUTSOURCE_SUBMITTED)).thenReturn(2L);
            when(financeReportMapper.selectAgentTasks(eq(99L), eq(null), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(financeReportMapper.selectAgentTasks(eq(99L), eq(ReportConstant.OUTSOURCE_PENDING), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(financeReportMapper.selectAgentTasks(eq(99L), eq(ReportConstant.OUTSOURCE_IN_PROGRESS), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(financeReportMapper.selectAgentTasks(eq(99L), eq(ReportConstant.OUTSOURCE_SUBMITTED), anyInt()))
                    .thenReturn(Collections.emptyList());

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(ReportConstant.ROLE_AGENT_ADMIN, vo.getRole());
            AgentDashboardVO agent = vo.getAgent();
            assertNotNull(agent);
            assertAll("代理商任务计数",
                    () -> assertEquals(10L, agent.getTotalCount()),
                    () -> assertEquals(3L, agent.getPendingCount()),
                    () -> assertEquals(4L, agent.getInProgressCount()),
                    () -> assertEquals(2L, agent.getSubmittedCount())
            );
        }

        @Test
        @DisplayName("agentCompanyId 为 null 时返回空数据，不查询任何 Mapper")
        void should_return_empty_when_tenant_id_null() {
            // AGENT_ENGINEER 角色但 tenantId 为空
            UserContext ctx = UserContext.builder()
                    .userId(41L).realName("代理商工程师").tenantType("AGENT")
                    .roles(List.of(ReportConstant.ROLE_AGENT_ENGINEER))
                    .build();
            UserContextHolder.set(ctx);

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(ReportConstant.ROLE_AGENT_ENGINEER, vo.getRole());
            AgentDashboardVO agent = vo.getAgent();
            assertNotNull(agent);
            assertAll("空数据",
                    () -> assertEquals(0L, agent.getTotalCount()),
                    () -> assertEquals(0L, agent.getPendingCount()),
                    () -> assertEquals(0L, agent.getInProgressCount()),
                    () -> assertEquals(0L, agent.getSubmittedCount()),
                    () -> assertEquals(0L, agent.getOverdueCount()),
                    () -> assertNotNull(agent.getPendingTasks()),
                    () -> assertNotNull(agent.getInProgressTasks()),
                    () -> assertNotNull(agent.getSubmittedTasks())
            );
            // 不应调用任何 Mapper
            verify(financeReportMapper, never()).countAgentTasks(any(), any());
            verify(financeReportMapper, never()).selectAgentTasks(any(), any(), anyInt());
        }

        @Test
        @DisplayName("超期数 = 全部任务列表中 overdue=1 的数量")
        void should_count_overdue_from_task_list() {
            UserContext ctx = UserContext.builder()
                    .userId(40L).realName("代理商").tenantType("AGENT").tenantId(99L)
                    .roles(List.of(ReportConstant.ROLE_AGENT_ADMIN))
                    .build();
            UserContextHolder.set(ctx);
            when(financeReportMapper.countAgentTasks(99L, null)).thenReturn(5L);
            when(financeReportMapper.countAgentTasks(99L, ReportConstant.OUTSOURCE_PENDING)).thenReturn(1L);
            when(financeReportMapper.countAgentTasks(99L, ReportConstant.OUTSOURCE_IN_PROGRESS)).thenReturn(2L);
            when(financeReportMapper.countAgentTasks(99L, ReportConstant.OUTSOURCE_SUBMITTED)).thenReturn(2L);
            // 全部任务列表中 3 条 overdue=1
            when(financeReportMapper.selectAgentTasks(eq(99L), eq(null), anyInt()))
                    .thenReturn(List.of(
                            buildOutsourceTask(1L, 1),
                            buildOutsourceTask(2L, 1),
                            buildOutsourceTask(3L, 1),
                            buildOutsourceTask(4L, 0)
                    ));
            when(financeReportMapper.selectAgentTasks(eq(99L), eq(ReportConstant.OUTSOURCE_PENDING), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(financeReportMapper.selectAgentTasks(eq(99L), eq(ReportConstant.OUTSOURCE_IN_PROGRESS), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(financeReportMapper.selectAgentTasks(eq(99L), eq(ReportConstant.OUTSOURCE_SUBMITTED), anyInt()))
                    .thenReturn(Collections.emptyList());

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(3L, vo.getAgent().getOverdueCount());
        }

        @Test
        @DisplayName("AGENT_ADMIN 优先级高于 AGENT_ENGINEER")
        void should_agent_admin_take_precedence_over_agent_engineer() {
            UserContext ctx = UserContext.builder()
                    .userId(42L).realName("代理商").tenantType("AGENT").tenantId(88L)
                    .roles(List.of(ReportConstant.ROLE_AGENT_ENGINEER, ReportConstant.ROLE_AGENT_ADMIN))
                    .build();
            UserContextHolder.set(ctx);
            when(financeReportMapper.countAgentTasks(any(), any())).thenReturn(0L);
            when(financeReportMapper.selectAgentTasks(any(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            DashboardVO vo = dashboardService.getDashboard();

            assertEquals(ReportConstant.ROLE_AGENT_ADMIN, vo.getRole());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private UserContext buildCtx(Long userId, String realName, List<String> roles) {
        return UserContext.builder()
                .userId(userId).realName(realName).roles(roles)
                .build();
    }

    private RiskProjectVO buildRisk(Long projectId) {
        RiskProjectVO r = new RiskProjectVO();
        r.setProjectId(projectId);
        r.setRiskType(ReportConstant.RISK_PROGRESS_DELAY);
        return r;
    }

    private OutsourceTaskItemVO buildOutsourceTask(Long id, Integer overdue) {
        OutsourceTaskItemVO t = new OutsourceTaskItemVO();
        t.setOutsourceTaskId(id);
        t.setOverdue(overdue);
        return t;
    }
}
