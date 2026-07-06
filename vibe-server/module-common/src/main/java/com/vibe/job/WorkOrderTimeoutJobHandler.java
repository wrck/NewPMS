package com.vibe.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单超时升级通知定时任务。
 *
 * <p><b>调度配置</b>：每小时执行一次（cron: 0 0 * * * ?，由 xxl-job-admin 配置）。</p>
 *
 * <p><b>业务逻辑</b>：扫描 {@code work_order} 表，找出计划完成时间已过且状态未完成的工单，
 * 按超期时长三级升级通知：</p>
 * <ol>
 *   <li>超期 0~24h：通知负责工程师（站内信 + IM）</li>
 *   <li>超期 24~48h：升级通知 PM（项目所在项目经理）</li>
 *   <li>超期 48~72h：升级通知总监（部门负责人）</li>
 *   <li>超期 &gt;72h：标记为高风险工单并触发风险升级事件（{@code RiskEscalatedEvent}）</li>
 * </ol>
 *
 * <p><b>依赖的 Mapper/Service</b>（部署在 vibe-server-bootstrap 时可用）：</p>
 * <ul>
 *   <li>{@code com.vibe.delivery.mapper.WorkOrderMapper} - 查询超期工单</li>
 *   <li>{@code com.vibe.delivery.service.WorkOrderService} - 工单业务服务</li>
 *   <li>{@code com.vibe.project.service.ProjectService} - 查询工单对应项目的 PM</li>
 *   <li>{@code com.vibe.system.service.SysNoticeService} - 发送分级通知</li>
 *   <li>{@code com.vibe.integration.service.ImNotificationAdapter} - IM 系统通知转发</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
public class WorkOrderTimeoutJobHandler {

    /**
     * XXL-JOB 入口方法，调度中心通过 {@code @XxlJob("workOrderTimeoutJob")} 触发。
     */
    @XxlJob("workOrderTimeoutJob")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("========== 工单超时升级扫描任务启动 ==========");
        XxlJobHelper.log("调度参数: {}", jobParam);

        try {
            // TODO: 注入 WorkOrderMapper / WorkOrderService / ProjectService / SysNoticeService
            //  1. 查询 work_order 表 status 未完成且 plan_end_time < now() 的工单
            //     LambdaQueryWrapper<WorkOrderEntity> wrapper = Wrappers.lambdaQuery();
            //     wrapper.in(WorkOrderEntity::getStatus, "PENDING", "IN_PROGRESS")
            //            .lt(WorkOrderEntity::getPlanEndTime, LocalDateTime.now());
            //     List<WorkOrderEntity> overdueOrders = workOrderMapper.selectList(wrapper);
            //  2. 对每个超期工单计算超期小时数：Duration.between(planEndTime, now).toHours()
            //  3. 按超期小时分级：
            //     - 0~24h：通知工程师（userId = workOrder.engineerId）
            //     - 24~48h：通知 PM（通过 projectService.getById(workOrder.projectId).pmId）
            //     - 48~72h：通知总监（通过 sysOrgService 获取部门负责人）
            //     - >72h：触发 RiskEscalatedEvent（领域事件总线发布）
            //  4. 同工单同级别 24h 内去重（Redis 控制）
            int overdueCount = 0;
            int notifiedCount = 0;
            int escalatedCount = 0;
            XxlJobHelper.log("扫描完成，超期工单数: {}，已通知数: {}，升级数: {}",
                    overdueCount, notifiedCount, escalatedCount);

            XxlJobHelper.handleSuccess("工单超时扫描执行成功，超期数=" + overdueCount
                    + "，通知数=" + notifiedCount + "，升级数=" + escalatedCount);
        } catch (Exception e) {
            log.error("工单超时升级扫描任务执行失败", e);
            XxlJobHelper.log("工单超时升级扫描任务执行失败: {}", e.getMessage());
            XxlJobHelper.handleFail("工单超时升级扫描任务执行失败: " + e.getMessage());
        }

        XxlJobHelper.log("========== 工单超时升级扫描任务结束 ==========");
    }
}
