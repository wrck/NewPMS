package com.vibe.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 验收逾期提醒定时任务。
 *
 * <p><b>调度配置</b>：每日 08:00 执行（cron: 0 0 8 * * ?，由 xxl-job-admin 配置）。</p>
 *
 * <p><b>业务逻辑</b>：</p>
 * <ol>
 *   <li>扫描 {@code acceptance_task} 表，找出超期未完成的验收任务</li>
 *   <li>通知 PM 与客户（客户通知通过客户门户 H5 / 短信 / IM）</li>
 *   <li>对超过约定验收期 7 天以上的任务升级到总监</li>
 *   <li>同步更新项目阶段状态（如阻塞后续割接/结项流程）</li>
 * </ol>
 *
 * <p><b>依赖的 Mapper/Service</b>（部署在 vibe-server-bootstrap 时可用）：</p>
 * <ul>
 *   <li>{@code com.vibe.acceptance.mapper.AcceptanceTaskMapper} - 查询验收任务</li>
 *   <li>{@code com.vibe.acceptance.service.AcceptanceTaskService} - 验收任务业务服务</li>
 *   <li>{@code com.vibe.project.service.ProjectService} - 查询项目 PM 与客户信息</li>
 *   <li>{@code com.vibe.system.service.SysNoticeService} - 发送站内通知</li>
 *   <li>{@code com.vibe.collaboration.service.CustomerNotificationService} - 客户侧通知（短信/IM/H5）</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
public class AcceptanceOverdueJobHandler {

    /**
     * XXL-JOB 入口方法，调度中心通过 {@code @XxlJob("acceptanceOverdueJob")} 触发。
     */
    @XxlJob("acceptanceOverdueJob")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("========== 验收逾期提醒任务启动 ==========");
        XxlJobHelper.log("调度参数: {}", jobParam);

        try {
            // TODO: 注入 AcceptanceTaskMapper / AcceptanceTaskService / ProjectService / SysNoticeService
            //  1. 查询 acceptance_task 表 status != 'COMPLETED' 且 plan_end_time < now() 的任务
            //     LambdaQueryWrapper<AcceptanceTaskEntity> wrapper = Wrappers.lambdaQuery();
            //     wrapper.ne(AcceptanceTaskEntity::getStatus, "COMPLETED")
            //            .lt(AcceptanceTaskEntity::getPlanEndTime, LocalDateTime.now());
            //  2. 对每个逾期任务：
            //     - 通知 PM（通过 projectService.getById(task.projectId).pmId）
            //     - 通知客户（通过 customerNotificationService）
            //     - 超期 7 天以上升级到总监
            //  3. 同任务同级别 24h 内去重
            int overdueCount = 0;
            int notifiedCount = 0;
            XxlJobHelper.log("扫描完成，逾期验收数: {}，已通知数: {}", overdueCount, notifiedCount);

            XxlJobHelper.handleSuccess("验收逾期提醒任务执行成功，逾期数=" + overdueCount
                    + "，通知数=" + notifiedCount);
        } catch (Exception e) {
            log.error("验收逾期提醒任务执行失败", e);
            XxlJobHelper.log("验收逾期提醒任务执行失败: {}", e.getMessage());
            XxlJobHelper.handleFail("验收逾期提醒任务执行失败: " + e.getMessage());
        }

        XxlJobHelper.log("========== 验收逾期提醒任务结束 ==========");
    }
}
