package com.vibe.report.mapper;

import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.DeliverableItemVO;
import com.vibe.report.vo.ProjectItemVO;
import com.vibe.report.vo.ProjectReportVO;
import com.vibe.report.vo.RiskProjectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目域报表 Mapper
 *
 * <p>由 {@code ReportMapper} 拆分而来，承担项目相关聚合查询：
 * 项目状态/阶段分布、月度趋势、风险项目、PM 业绩、PM 维度计数、PM 待审核交付物等。</p>
 *
 * <p>SQL 统一在 XML（{@code mapper/report/ProjectReportMapper.xml}）中维护。</p>
 *
 * @author vibe
 */
@Mapper
public interface ProjectReportMapper {

    /* ============ 项目统计聚合 ============ */

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

    /* ============ 风险项目 ============ */

    /**
     * 查询风险项目（进度滞后/超期任务/未解决问题/项目超期）
     *
     * @param limit 返回条数上限
     * @return 风险项目列表
     */
    List<RiskProjectVO> selectRiskProjects(@Param("limit") int limit);

    /* ============ PM 维度查询 ============ */

    /**
     * 查询 PM 负责的项目列表
     *
     * @param userId PM 用户ID
     * @param limit  返回条数上限
     * @return 项目列表
     */
    List<ProjectItemVO> selectMyProjects(@Param("userId") Long userId,
                                         @Param("limit") int limit);

    /**
     * 查询 PM 待审核交付物（PM 负责项目下，转包任务状态为 SUBMITTED 的最新交付物）
     *
     * @param userId PM 用户ID
     * @param status 转包任务状态（SUBMITTED）
     * @param limit  返回条数上限
     * @return 交付物列表
     */
    List<DeliverableItemVO> selectMyDeliverables(@Param("userId") Long userId,
                                                 @Param("status") String status,
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

    /* ============ 总监审批待办计数 ============ */

    /**
     * 统计待审批项目变更数（状态 PENDING）
     */
    Long countPendingChangeLogs();

    /* ============ 环比/活跃/总数 ============ */

    /**
     * 统计指定日期之前的项目数（环比基准）
     *
     * @param beforeDate 截止日期（不含当天，即 create_time < beforeDate 00:00:00）
     * @return 项目数
     */
    Long countProjectsBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 统计进行中项目数（非终态）
     */
    Long countActiveProjects();

    /**
     * 统计项目总数
     */
    Long countAllProjects();

    /* ============ 业务报表（项目） ============ */

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
}
