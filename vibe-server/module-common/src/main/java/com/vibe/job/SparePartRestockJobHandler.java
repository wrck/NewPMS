package com.vibe.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 备件补货提醒定时任务。
 *
 * <p><b>调度配置</b>：每周一 09:00 执行（cron: 0 0 9 ? * MON，由 xxl-job-admin 配置）。</p>
 *
 * <p><b>业务逻辑</b>：</p>
 * <ol>
 *   <li>扫描 {@code spare_part} 表数量低于阈值的备件（{@code quantity &lt; restock_threshold}）</li>
 *   <li>生成补货提醒记录（{@code spare_part_restock_reminder}，待建表）</li>
 *   <li>通知仓库管理员/采购负责人</li>
 *   <li>对长期未补货的备件升级通知到采购主管</li>
 * </ol>
 *
 * <p><b>依赖的 Mapper/Service</b>（部署在 vibe-server-bootstrap 时可用）：</p>
 * <ul>
 *   <li>{@code com.vibe.device.mapper.SparePartMapper} - 查询备件数量与补货阈值</li>
 *   <li>{@code com.vibe.device.service.SparePartService} - 备件业务服务</li>
 *   <li>{@code com.vibe.system.service.SysNoticeService} - 发送补货提醒通知</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
public class SparePartRestockJobHandler {

    /**
     * XXL-JOB 入口方法，调度中心通过 {@code @XxlJob("sparePartRestockJob")} 触发。
     */
    @XxlJob("sparePartRestockJob")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("========== 备件补货提醒任务启动 ==========");
        XxlJobHelper.log("调度参数: {}", jobParam);

        try {
            // TODO: 注入 SparePartMapper / SparePartService
            //  1. 查询 spare_part 表所有数量低于 restock_threshold 的备件
            //     List<SparePartEntity> lowStockParts = sparePartMapper.selectList(
            //         new LambdaQueryWrapper<SparePartEntity>().lt(SparePartEntity::getQuantity, ...));
            //  2. 过滤未关闭的补货提醒（避免重复提醒）
            //  3. 生成补货提醒记录（spare_part_restock_reminder 表，待建表）
            //  4. 调用 SysNoticeService 发送补货通知给仓库管理员/采购负责人
            //  5. 7 天内已提醒但仍未补货的备件，升级通知到采购主管
            int reminderCount = 0;
            XxlJobHelper.log("扫描完成，新增补货提醒数: {}", reminderCount);

            XxlJobHelper.handleSuccess("备件补货提醒任务执行成功，提醒数=" + reminderCount);
        } catch (Exception e) {
            log.error("备件补货提醒任务执行失败", e);
            XxlJobHelper.log("备件补货提醒任务执行失败: {}", e.getMessage());
            XxlJobHelper.handleFail("备件补货提醒任务执行失败: " + e.getMessage());
        }

        XxlJobHelper.log("========== 备件补货提醒任务结束 ==========");
    }
}
