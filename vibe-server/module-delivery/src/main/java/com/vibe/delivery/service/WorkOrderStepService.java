package com.vibe.delivery.service;

import com.vibe.delivery.dto.WorkOrderStepCompleteDTO;
import com.vibe.delivery.vo.WorkOrderStepVO;

import java.util.List;

/**
 * 工单施工步骤服务
 *
 * @author vibe
 */
public interface WorkOrderStepService {

    /**
     * 初始化施工步骤（按任务配置的标准步骤列表）
     *
     * @param workOrderId 工单ID
     * @param stepNames   步骤名称列表
     */
    void initSteps(Long workOrderId, List<String> stepNames);

    /**
     * 查询工单的施工步骤列表（按 step_no 排序）
     */
    List<WorkOrderStepVO> listByWorkOrder(Long workOrderId);

    /**
     * 标记步骤完成 / 跳过：
     * <ul>
     *   <li>状态 WAITING → COMPLETED / SKIPPED</li>
     *   <li>记录完成时间与耗时（秒，相对工单签到时间）</li>
     *   <li>所有步骤完成后自动将工单状态推进到 COMPLETED 候选（需工程师签退后确认）</li>
     * </ul>
     *
     * @return 更新后的步骤视图
     */
    WorkOrderStepVO completeStep(Long workOrderId, Long stepId, WorkOrderStepCompleteDTO dto);

    /**
     * 计算工单步骤进度
     *
     * @return 数组 [completedCount, totalCount]
     */
    int[] calculateProgress(Long workOrderId);
}
