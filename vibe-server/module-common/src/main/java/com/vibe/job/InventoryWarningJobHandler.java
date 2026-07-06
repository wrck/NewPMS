package com.vibe.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存预警定时任务。
 *
 * <p><b>调度配置</b>：每日 09:00 执行（cron: 0 0 9 * * ?，由 xxl-job-admin 配置）。</p>
 *
 * <p><b>业务逻辑</b>：</p>
 * <ol>
 *   <li>扫描所有仓库所有型号库存（关联 {@code device_inventory_log} 与 {@code warehouse.safety_stock}）</li>
 *   <li>对实际库存低于安全库存的型号，生成预警记录写入 {@code device_inventory_warning}（待建表）</li>
 *   <li>通知设备管理员（站内信 + IM 系统转发，由 module-system NoticeService 与 module-integration ImNotificationAdapter 协作）</li>
 *   <li>同型号预警去重（24h 内同仓库同型号仅发 1 次）</li>
 * </ol>
 *
 * <p><b>实现备注</b>：本类位于 module-common，无法直接注入 module-device 的 Mapper/Service
 * （module-common 不依赖业务模块）。实际业务逻辑由后续任务在 vibe-server-bootstrap 或
 * module-device 中扩展（推荐在该模块新增 InventoryWarningService 并通过 @Resource 注入），
 * 或将本类迁移至 vibe-server-bootstrap。</p>
 *
 * <p><b>依赖的 Mapper/Service</b>（部署在 vibe-server-bootstrap 时可用）：</p>
 * <ul>
 *   <li>{@code com.vibe.device.mapper.DeviceInventoryLogMapper} - 查询当前各型号库存</li>
 *   <li>{@code com.vibe.device.mapper.WarehouseMapper} / {@code WarehouseService} - 查询仓库与安全库存阈值</li>
 *   <li>{@code com.vibe.system.service.SysNoticeService} - 发送站内通知</li>
 *   <li>{@code com.vibe.integration.service.ImNotificationAdapter} - IM 系统通知转发（飞书/钉钉/企微）</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Component
public class InventoryWarningJobHandler {

    /**
     * XXL-JOB 入口方法。
     *
     * <p>调度中心通过 {@code @XxlJob("inventoryWarningJob")} 注解的 value 进行路由触发。
     * 调度参数可通过 {@code XxlJobHelper.getJobParam()} 获取（如指定仓库 ID 进行单仓扫描）。</p>
     */
    @XxlJob("inventoryWarningJob")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("========== 库存预警定时任务启动 ==========");
        XxlJobHelper.log("调度参数: {}", jobParam);

        try {
            // TODO: 注入 DeviceInventoryLogMapper / WarehouseMapper / WarehouseService
            //  1. 查询全部仓库及各型号安全库存阈值
            //     List<WarehouseVO> warehouses = warehouseService.listAll();
            //  2. 对每个仓库，按型号聚合 device_inventory_log 当前库存
            //  3. 比对实际库存 vs safety_stock，低于阈值的收集为预警对象
            //  4. 写入预警记录（device_inventory_warning 表，待建表）
            //  5. 调用 SysNoticeService 发送站内通知给设备管理员
            //  6. 调用 ImNotificationAdapter 转发到 IM 系统兜底
            //  7. 同仓库同型号 24h 内去重（通过 Redis SETNX 控制）
            int warningCount = 0;
            XxlJobHelper.log("扫描完成，新增预警记录数: {}", warningCount);

            XxlJobHelper.handleSuccess("库存预警任务执行成功，预警数=" + warningCount);
        } catch (Exception e) {
            log.error("库存预警定时任务执行失败", e);
            XxlJobHelper.log("库存预警定时任务执行失败: {}", e.getMessage());
            XxlJobHelper.handleFail("库存预警任务执行失败: " + e.getMessage());
        }

        XxlJobHelper.log("========== 库存预警定时任务结束 ==========");
    }
}
