package com.vibe.report.mapper;

import com.vibe.report.vo.ChartDataVO;
import com.vibe.report.vo.FinanceReportVO;
import com.vibe.report.vo.OutsourceTaskItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 财务域报表 Mapper
 *
 * <p>由 {@code ReportMapper} 拆分而来，承担财务 + 代理商相关聚合查询：
 * 利润趋势、收入成本对比、客户占比、代理商结算、代理商任务统计等。</p>
 *
 * <p>SQL 统一在 XML（{@code mapper/report/FinanceReportMapper.xml}）中维护。</p>
 *
 * @author vibe
 */
@Mapper
public interface FinanceReportMapper {

    /* ============ 代理商统计聚合 ============ */

    /**
     * 统计代理商总数与活跃数
     *
     * @return name=TOTAL/ACTIVE，value=数量
     */
    List<ChartDataVO> countAgentCompanies();

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

    /* ============ 总监审批待办计数（工作量确认属财务域） ============ */

    /**
     * 统计待确认工作量数（状态 SUBMITTED）
     */
    Long countPendingWorkloads();

    /* ============ 环比/活跃/总数 ============ */

    /**
     * 统计指定日期之前的代理商数
     */
    Long countAgentCompaniesBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 统计活跃代理商数（状态 ACTIVE）
     */
    Long countActiveAgentCompanies();

    /**
     * 统计代理商总数
     */
    Long countAllAgentCompanies();

    /* ============ 业务报表（财务） ============ */

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
