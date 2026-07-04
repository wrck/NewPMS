package com.vibe.report.mapper;

import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.DeliverableItemVO;
import com.vibe.report.vo.DeviceReportVO;
import com.vibe.report.vo.FinanceReportVO;
import com.vibe.report.vo.OutsourceTaskItemVO;
import com.vibe.report.vo.ProjectItemVO;
import com.vibe.report.vo.ProjectReportVO;
import com.vibe.report.vo.ResourceReportVO;
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

    /* ============ 业务报表（项目/设备/资源/财务） ============ */

    /**
     * 项目报表 - 汇总指标（按筛选条件）
     *
     * @param status      状态（可空）
     * @param pmId        PM 用户ID（可空）
     * @param productLine 产品线（可空）
     * @param region      区域（可空）
     * @return 单行汇总：total/completed/ongoing/overdue/avgProgress
     */
    ProjectReportVO.Summary projectReportSummary(@Param("status") String status,
                                                  @Param("pmId") Long pmId,
                                                  @Param("productLine") String productLine,
                                                  @Param("region") String region);

    /**
     * 项目报表 - 按状态分组
     */
    List<ProjectReportVO.StatusStat> projectReportByStatus(@Param("status") String status,
                                                            @Param("pmId") Long pmId,
                                                            @Param("productLine") String productLine,
                                                            @Param("region") String region);

    /**
     * 项目报表 - 按产品线分组
     */
    List<ProjectReportVO.ProductLineStat> projectReportByProductLine(@Param("status") String status,
                                                                     @Param("pmId") Long pmId,
                                                                     @Param("productLine") String productLine,
                                                                     @Param("region") String region);

    /**
     * 项目报表 - 按区域分组
     */
    List<ProjectReportVO.RegionStat> projectReportByRegion(@Param("status") String status,
                                                            @Param("pmId") Long pmId,
                                                            @Param("productLine") String productLine,
                                                            @Param("region") String region);

    /**
     * 项目报表 - PM 业绩统计
     */
    List<ProjectReportVO.PmStat> projectReportByPm(@Param("status") String status,
                                                    @Param("pmId") Long pmId,
                                                    @Param("productLine") String productLine,
                                                    @Param("region") String region);

    /**
     * 项目报表 - 明细列表（含超期标识）
     */
    List<ProjectReportVO.ProjectDetail> projectReportDetail(@Param("status") String status,
                                                             @Param("pmId") Long pmId,
                                                             @Param("productLine") String productLine,
                                                             @Param("region") String region);

    /**
     * 设备报表 - 汇总指标
     */
    DeviceReportVO.Summary deviceReportSummary(@Param("productLine") String productLine);

    /**
     * 设备报表 - 按状态分组
     */
    List<DeviceReportVO.StatusDist> deviceReportByStatus(@Param("productLine") String productLine);

    /**
     * 设备报表 - 按产品线分组（关联 device_model）
     */
    List<DeviceReportVO.ProductLineDist> deviceReportByProductLine(@Param("productLine") String productLine);

    /**
     * 设备报表 - 各项目 BOM 完成率
     */
    List<DeviceReportVO.BomCompletion> deviceReportBomCompletion();

    /**
     * 设备报表 - 库存状态（按仓库汇总设备数）
     */
    List<DeviceReportVO.InventoryStatus> deviceReportInventory();

    /**
     * 资源报表 - 汇总指标
     */
    ResourceReportVO.Summary resourceReportSummary(@Param("engineerId") Long engineerId);

    /**
     * 资源报表 - 工程师维度统计（任务数/工时/加班/利用率/按时率）
     */
    List<ResourceReportVO.EngineerStat> resourceReportByEngineer(@Param("engineerId") Long engineerId);

    /**
     * 资源报表 - 项目维度统计
     */
    List<ResourceReportVO.ProjectStat> resourceReportByProject();

    /**
     * 财务报表 - 汇总指标（预算作为收入代理，成本来自 finance_cost）
     */
    FinanceReportVO.Summary financeReportSummary(@Param("customerId") Long customerId);

    /**
     * 财务报表 - 按客户维度利润
     */
    List<FinanceReportVO.CustomerProfit> financeReportByCustomer(@Param("customerId") Long customerId);

    /**
     * 财务报表 - 按区域维度利润
     */
    List<FinanceReportVO.RegionProfit> financeReportByRegion();

    /**
     * 财务报表 - 按产品线维度利润
     */
    List<FinanceReportVO.ProductLineProfit> financeReportByProductLine();

    /**
     * 财务报表 - 代理商结算汇总（来自 finance_workload_confirmation）
     */
    List<FinanceReportVO.AgentSettlement> financeReportAgentSettlement();
}
