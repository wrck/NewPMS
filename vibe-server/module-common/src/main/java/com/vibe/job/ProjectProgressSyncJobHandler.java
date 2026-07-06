package com.vibe.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 项目进度同步定时任务。
 *
 * <p><b>调度配置</b>：每小时执行一次（cron: 0 0 * * * ?，由 xxl-job-admin 配置）。</p>
 *
 * <p><b>业务逻辑</b>：基于 {@code project_task} 表的任务完成率，
 * 同步刷新 {@code project.progress} 字段（百分比 0~100）。</p>
 *
 * <p>进度计算规则：</p>
 * <ol>
 *   <li>统计项目下全部任务数 totalTasks（{@code project_task.project_id = ?}）</li>
 *   <li>统计已完成任务数 completedTasks（{@code status = 'COMPLETED'}）</li>
 *   <li>项目进度 = round(completedTasks / totalTasks * 100, 2)</li>
 *   <li>当 progress = 100 时联动触发项目状态机（{@code IN_PROGRESS → ACCEPTING}）</li>
 * </ol>
 *
 * <p><b>依赖的 Mapper/Service</b>（部署在 vibe-server-bootstrap 时可用）：</p>
 * <ul>
 *   <li>{@code com.vibe.project.mapper.ProjectTaskMapper} - 任务统计</li>
 *   <li>{@code com.vibe.project.mapper.ProjectMapper} - 更新 project.progress</li>
 *   <li>{@code com.vibe.project.service.ProjectService} - 项目状态机联动</li>
 *   <li>{@code com.vibe.common.event.DomainEventPublisher} - 进度变更事件投递</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
public class ProjectProgressSyncJobHandler {

    /**
     * XXL-JOB 入口方法，调度中心通过 {@code @XxlJob("projectProgressSyncJob")} 触发。
     */
    @XxlJob("projectProgressSyncJob")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("========== 项目进度同步任务启动 ==========");
        XxlJobHelper.log("调度参数: {}", jobParam);

        try {
            // TODO: 注入 ProjectTaskMapper / ProjectMapper / ProjectService
            //  1. 查询所有未结项项目（status IN ('PLANNED', 'IN_PROGRESS')）
            //     List<ProjectEntity> projects = projectMapper.selectList(
            //         Wrappers.lambdaQuery<ProjectEntity>()
            //             .in(ProjectEntity::getStatus, "PLANNED", "IN_PROGRESS"));
            //  2. 对每个项目计算进度：
            //     long total = projectTaskMapper.selectCount(
            //         Wrappers.lambdaQuery<ProjectTaskEntity>()
            //             .eq(ProjectTaskEntity::getProjectId, project.getId()));
            //     long completed = projectTaskMapper.selectCount(
            //         Wrappers.lambdaQuery<ProjectTaskEntity>()
            //             .eq(ProjectTaskEntity::getProjectId, project.getId())
            //             .eq(ProjectTaskEntity::getStatus, "COMPLETED"));
            //     BigDecimal progress = total > 0
            //         ? BigDecimal.valueOf(completed * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
            //         : BigDecimal.ZERO;
            //  3. 仅当 progress 与原值不同时更新（避免无意义 SQL）
            //  4. progress 达 100 时调用 projectService.transitionToStatus(projectId, "ACCEPTING")
            //  5. 投递 ProjectStatusChangedEvent（领域事件总线）
            int syncedCount = 0;
            XxlJobHelper.log("进度同步完成，更新项目数: {}", syncedCount);

            XxlJobHelper.handleSuccess("项目进度同步任务执行成功，更新数=" + syncedCount);
        } catch (Exception e) {
            log.error("项目进度同步任务执行失败", e);
            XxlJobHelper.log("项目进度同步任务执行失败: {}", e.getMessage());
            XxlJobHelper.handleFail("项目进度同步任务执行失败: " + e.getMessage());
        }

        XxlJobHelper.log("========== 项目进度同步任务结束 ==========");
    }
}
