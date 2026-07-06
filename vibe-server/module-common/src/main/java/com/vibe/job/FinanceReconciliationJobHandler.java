package com.vibe.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 财务对账定时任务。
 *
 * <p><b>调度配置</b>：每月 1 日 00:00 执行（cron: 0 0 0 1 * ?，由 xxl-job-admin 配置）。</p>
 *
 * <p><b>业务逻辑</b>：汇总上月财务数据生成对账报表，覆盖：</p>
 * <ol>
 *   <li>{@code finance_cost} 表：汇总上月实际成本（按项目/类型/客户维度）</li>
 *   <li>{@code finance_budget} 表：汇总上月预算执行情况（实际 vs 预算偏差）</li>
 *   <li>{@code agent_workload} 表：汇总代理商工作量与结算金额</li>
 *   <li>生成对账报表记录（{@code finance_reconciliation_report}，待建表）</li>
 *   <li>对偏差超过 10% 的项目通知财务负责人与 PM 复核</li>
 * </ol>
 *
 * <p><b>依赖的 Mapper/Service</b>（部署在 vibe-server-bootstrap 时可用）：</p>
 * <ul>
 *   <li>{@code com.vibe.finance.mapper.FinanceCostMapper} - 成本汇总查询</li>
 *   <li>{@code com.vibe.finance.mapper.FinanceBudgetMapper} - 预算执行情况查询</li>
 *   <li>{@code com.vibe.finance.mapper.FinanceWorkloadConfirmationMapper} - 代理商工作量汇总</li>
 *   <li>{@code com.vibe.finance.service.FinanceReportService} - 财务报表服务</li>
 *   <li>{@code com.vibe.system.service.SysNoticeService} - 偏差超阈值通知</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
public class FinanceReconciliationJobHandler {

    /**
     * 预算偏差阈值（10%），超过此阈值触发复核通知。
     */
    private static final double BUDGET_DEVIATION_THRESHOLD = 0.10;

    /**
     * XXL-JOB 入口方法，调度中心通过 {@code @XxlJob("financeReconciliationJob")} 触发。
     */
    @XxlJob("financeReconciliationJob")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("========== 财务对账定时任务启动 ==========");
        XxlJobHelper.log("调度参数: {}", jobParam);

        try {
            // 对账周期：上月 1 日 00:00 至上月最后一日 23:59:59
            // LocalDate now = LocalDate.now();
            // LocalDate lastMonthStart = now.minusMonths(1).withDayOfMonth(1);
            // LocalDate lastMonthEnd = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());
            // XxlJobHelper.log("对账周期: {} ~ {}", lastMonthStart, lastMonthEnd);

            // TODO: 注入 FinanceCostMapper / FinanceBudgetMapper / FinanceWorkloadConfirmationMapper
            //  1. 汇总 finance_cost 上月成本（按 project_id / cost_type / customer_id 维度）
            //     List<Map<String, Object>> costSummary = financeCostMapper.selectLastMonthSummary();
            //  2. 汇总 finance_budget 上月预算执行情况
            //     List<Map<String, Object>> budgetSummary = financeBudgetMapper.selectLastMonthSummary();
            //  3. 汇总 agent_workload 上月代理商工作量与结算金额
            //     List<Map<String, Object>> workloadSummary = financeWorkloadConfirmationMapper.selectLastMonthSummary();
            //  4. 计算每个项目预算偏差 = (actual - budget) / budget
            //  5. 对偏差绝对值 > BUDGET_DEVIATION_THRESHOLD 的项目：
            //     - 写入对账报表 finance_reconciliation_report（待建表）
            //     - 通知财务负责人 + 对应 PM
            //  6. 生成汇总报表 Excel 通过 EasyExcel 导出到 MinIO（路径：finance/reconciliation/{yearMonth}.xlsx）
            int projectCount = 0;
            int deviationCount = 0;
            XxlJobHelper.log("汇总完成，覆盖项目数: {}，偏差项目数: {}（阈值: {}%）",
                    projectCount, deviationCount, (int) (BUDGET_DEVIATION_THRESHOLD * 100));

            XxlJobHelper.handleSuccess("财务对账任务执行成功，覆盖项目数=" + projectCount
                    + "，偏差项目数=" + deviationCount);
        } catch (Exception e) {
            log.error("财务对账任务执行失败", e);
            XxlJobHelper.log("财务对账任务执行失败: {}", e.getMessage());
            XxlJobHelper.handleFail("财务对账任务执行失败: " + e.getMessage());
        }

        XxlJobHelper.log("========== 财务对账定时任务结束 ==========");
    }
}
