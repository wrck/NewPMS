package com.vibe.report.mapper;

import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.DeliverableItemVO;
import com.vibe.report.vo.OutsourceTaskItemVO;
import com.vibe.report.vo.ProjectItemVO;
import com.vibe.report.vo.RiskProjectVO;
import com.vibe.report.vo.TaskItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 报表分析 Mapper
 *
 * <p>聚合统计查询（GROUP BY / COUNT / JOIN），SQL 统一在 XML 中维护。</p>
 *
 * @author vibe
 */
@Mapper
public interface ReportMapper {

    /* ============ 项目统计 ============ */

    /**
     * 按状态统计项目数
     *
     * @return 每个状态对应的项目数，name=状态码，value=数量
     */
    List<ChartDataVO> countProjectsByStatus();

    /**
     * 按当前阶段统计项目数
     *
     * @return 每个阶段对应的项目数，name=阶段码，value=数量
     */
    List<ChartDataVO> countProjectsByPhase();

    /**
     * 按月统计指定年份新增/完成项目数
     *
     * @param year 年份（如 2026）
     * @return 每月新增与完成数，month=月份，newCount=新增，completedCount=完成
     */
    List<ChartDataVO> countMonthlyProjects(@Param("year") Integer year);

    /* ============ 设备统计 ============ */

    /**
     * 按状态统计设备数
     *
     * @return 每个状态对应的设备数
     */
    List<ChartDataVO> countDevicesByStatus();

    /* ============ 工程师统计 ============ */

    /**
     * 统计工程师总数与活跃数
     *
     * @return name=TOTAL/ACTIVE，value=数量
     */
    List<ChartDataVO> countEngineers();

    /* ============ 代理商统计 ============ */

    /**
     * 统计代理商总数与活跃数
     *
     * @return name=TOTAL/ACTIVE，value=数量
     */
    List<ChartDataVO> countAgentCompanies();

    /* ============ 风险项目 ============ */

    /**
     * 查询风险项目（进度滞后/超期任务/未解决问题/项目超期）
     *
     * @param limit 返回条数上限
     * @return 风险项目列表
     */
    List<RiskProjectVO> selectRiskProjects(@Param("limit") int limit);

    /* ============ 用户维度查询 ============ */

    /**
     * 查询用户任务（按状态过滤）
     *
     * @param userId 用户ID
     * @param status 任务状态（为 null 时查全部）
     * @param limit  返回条数上限
     * @return 任务列表
     */
    List<TaskItemVO> selectMyTasks(@Param("userId") Long userId,
                                   @Param("status") String status,
                                   @Param("limit") int limit);

    /**
     * 查询用户项目（PM 负责的项目 + 项目成员关联的项目）
     *
     * @param userId 用户ID
     * @param limit  返回条数上限
     * @return 项目列表
     */
    List<ProjectItemVO> selectMyProjects(@Param("userId") Long userId,
                                         @Param("limit") int limit);

    /**
     * 查询用户待审核交付物（PM 负责的项目下，转包任务状态为 SUBMITTED 的交付物）
     *
     * @param userId 用户ID（PM）
     * @param status 转包任务状态（SUBMITTED）
     * @param limit  返回条数上限
     * @return 交付物列表
     */
    List<DeliverableItemVO> selectMyDeliverables(@Param("userId") Long userId,
                                                 @Param("status") String status,
                                                 @Param("limit") int limit);

    /**
     * 查询用户超期任务（计划结束日期 < 当天且未完成）
     *
     * @param userId 用户ID
     * @param today  当前日期
     * @param limit  返回条数上限
     * @return 超期任务列表
     */
    List<TaskItemVO> selectOverdueTasks(@Param("userId") Long userId,
                                        @Param("today") LocalDate today,
                                        @Param("limit") int limit);

    /* ============ PM 维度计数 ============ */

    /**
     * 统计 PM 负责的项目数
     */
    Long countProjectsByPm(@Param("userId") Long userId);

    /**
     * 统计 PM 负责的进行中项目数
     */
    Long countActiveProjectsByPm(@Param("userId") Long userId);

    /**
     * 统计 PM 负责项目下待派单任务数（状态 PENDING）
     */
    Long countPendingDispatchByPm(@Param("userId") Long userId);

    /**
     * 统计 PM 负责项目下待审核交付物数（转包任务状态 SUBMITTED）
     */
    Long countPendingReviewByPm(@Param("userId") Long userId);

    /* ============ 工程师维度计数 ============ */

    /**
     * 统计工程师今日任务数（计划开始 <= 今天 <= 计划结束，且未完成）
     */
    Long countTodayTasksByEngineer(@Param("userId") Long userId, @Param("today") LocalDate today);

    /**
     * 统计工程师待处理任务数（PENDING/ASSIGNED/IN_PROGRESS）
     */
    Long countPendingTasksByEngineer(@Param("userId") Long userId);

    /**
     * 统计工程师超期任务数
     */
    Long countOverdueTasksByEngineer(@Param("userId") Long userId, @Param("today") LocalDate today);

    /**
     * 统计工程师工时（指定日期区间合计）
     *
     * @param userId   用户ID
     * @param startDay 起始日期（含）
     * @param endDay   结束日期（含）
     * @return 工时合计（小时），无数据返回 null
     */
    BigDecimal sumWorkHours(@Param("userId") Long userId,
                            @Param("startDay") LocalDate startDay,
                            @Param("endDay") LocalDate endDay);

    /* ============ 代理商维度查询 ============ */

    /**
     * 统计代理商公司任务数（按状态）
     *
     * @param agentCompanyId 代理商公司ID
     * @param status         转包任务状态（为 null 时查全部）
     * @return 任务数
     */
    Long countAgentTasks(@Param("agentCompanyId") Long agentCompanyId,
                         @Param("status") String status);

    /**
     * 查询代理商公司任务列表（按状态过滤）
     *
     * @param agentCompanyId 代理商公司ID
     * @param status         转包任务状态（为 null 时查全部）
     * @param limit          返回条数上限
     * @return 任务列表
     */
    List<OutsourceTaskItemVO> selectAgentTasks(@Param("agentCompanyId") Long agentCompanyId,
                                               @Param("status") String status,
                                               @Param("limit") int limit);

    /* ============ 审批待办计数（总监） ============ */

    /**
     * 统计待审批项目变更数（状态 PENDING）
     */
    Long countPendingChangeLogs();

    /**
     * 统计待确认工作量数（状态 SUBMITTED）
     */
    Long countPendingWorkloads();

    /* ============ 环比统计 ============ */

    /**
     * 统计指定日期之前的项目数（环比基准）
     *
     * @param beforeDate 截止日期（不含当天，即 create_time < beforeDate 00:00:00）
     * @return 项目数
     */
    Long countProjectsBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 统计指定日期之前的设备数
     */
    Long countDevicesBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 统计指定日期之前的工程师数
     */
    Long countEngineersBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 统计指定日期之前的代理商数
     */
    Long countAgentCompaniesBefore(@Param("beforeDate") LocalDate beforeDate);

    /* ============ 活跃数统计 ============ */

    /**
     * 统计进行中项目数（非终态）
     */
    Long countActiveProjects();

    /**
     * 统计在网设备数（状态 ONLINE）
     */
    Long countOnlineDevices();

    /**
     * 统计在职工程师数（状态 ACTIVE）
     */
    Long countActiveEngineers();

    /**
     * 统计活跃代理商数（状态 ACTIVE）
     */
    Long countActiveAgentCompanies();

    /* ============ 总数统计 ============ */

    /**
     * 统计项目总数
     */
    Long countAllProjects();

    /**
     * 统计设备总数
     */
    Long countAllDevices();

    /**
     * 统计工程师总数
     */
    Long countAllEngineers();

    /**
     * 统计代理商总数
     */
    Long countAllAgentCompanies();
}
