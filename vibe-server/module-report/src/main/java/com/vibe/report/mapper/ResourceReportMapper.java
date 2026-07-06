package com.vibe.report.mapper;

import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.ResourceReportVO;
import com.vibe.report.vo.TaskItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 资源域报表 Mapper
 *
 * <p>由 {@code ReportMapper} 拆分而来，承担资源（工程师）相关聚合查询：
 * 工程师负荷、工时统计、工程师维度任务计数、工程师列表查询等。</p>
 *
 * <p>SQL 统一在 XML（{@code mapper/report/ResourceReportMapper.xml}）中维护。</p>
 *
 * @author vibe
 */
@Mapper
public interface ResourceReportMapper {

    /* ============ 工程师统计聚合 ============ */

    /**
     * 统计工程师总数与活跃数
     *
     * @return name=TOTAL/ACTIVE，value=数量
     */
    List<ChartDataVO> countEngineers();

    /* ============ 工程师维度查询 ============ */

    /**
     * 查询工程师任务（按状态过滤）
     *
     * @param userId 工程师用户ID
     * @param status 任务状态（为 null 时查全部）
     * @param limit  返回条数上限
     * @return 任务列表
     */
    List<TaskItemVO> selectMyTasks(@Param("userId") Long userId,
                                   @Param("status") String status,
                                   @Param("limit") int limit);

    /**
     * 查询工程师超期任务（计划结束日期 < 当天且未完成）
     *
     * @param userId 工程师用户ID
     * @param today  当前日期
     * @param limit  返回条数上限
     * @return 超期任务列表
     */
    List<TaskItemVO> selectOverdueTasks(@Param("userId") Long userId,
                                        @Param("today") LocalDate today,
                                        @Param("limit") int limit);

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

    /* ============ 环比/活跃/总数 ============ */

    /**
     * 统计指定日期之前的工程师数
     */
    Long countEngineersBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 统计在职工程师数（状态 ACTIVE）
     */
    Long countActiveEngineers();

    /**
     * 统计工程师总数
     */
    Long countAllEngineers();

    /* ============ 业务报表（资源） ============ */

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
}
