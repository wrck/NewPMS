package com.vibe.delivery.service.impl;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.delivery.constant.DeliveryConstant;
import com.vibe.delivery.dto.WorkOrderStepCompleteDTO;
import com.vibe.delivery.entity.WorkOrderEntity;
import com.vibe.delivery.entity.WorkOrderStepEntity;
import com.vibe.delivery.mapper.WorkOrderMapper;
import com.vibe.delivery.mapper.WorkOrderStepMapper;
import com.vibe.delivery.service.WorkOrderStepService;
import com.vibe.delivery.vo.WorkOrderStepVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单施工步骤服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderStepServiceImpl implements WorkOrderStepService {

    private final WorkOrderStepMapper workOrderStepMapper;
    private final WorkOrderMapper workOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initSteps(Long workOrderId, List<String> stepNames) {
        if (workOrderId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工单ID不能为空");
        }
        if (CollectionUtils.isEmpty(stepNames)) {
            return;
        }
        for (int i = 0; i < stepNames.size(); i++) {
            WorkOrderStepEntity step = new WorkOrderStepEntity();
            step.setWorkOrderId(workOrderId);
            step.setStepNo(i + 1);
            step.setStepName(stepNames.get(i));
            step.setStatus(DeliveryConstant.STEP_STATUS_WAITING);
            workOrderStepMapper.insert(step);
        }
    }

    @Override
    public List<WorkOrderStepVO> listByWorkOrder(Long workOrderId) {
        return workOrderStepMapper.selectByWorkOrderId(workOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderStepVO completeStep(Long workOrderId, Long stepId, WorkOrderStepCompleteDTO dto) {
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw BusinessException.of(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        // 签到后才可标记步骤完成
        String status = workOrder.getStatus();
        if (DeliveryConstant.WORK_ORDER_STATUS_CREATED.equals(status)) {
            throw BusinessException.stateNotAllowed("请先签到后再标记施工步骤");
        }
        if (DeliveryConstant.WORK_ORDER_STATUS_COMPLETED.equals(status)
                || DeliveryConstant.WORK_ORDER_STATUS_CONFIRMED.equals(status)) {
            throw BusinessException.stateNotAllowed("工单已完成，不可再修改步骤");
        }

        WorkOrderStepEntity step = workOrderStepMapper.selectById(stepId);
        if (step == null || !workOrderId.equals(step.getWorkOrderId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "施工步骤不存在或不属于该工单");
        }
        if (!DeliveryConstant.STEP_STATUS_WAITING.equals(step.getStatus())) {
            throw BusinessException.stateNotAllowed("当前步骤状态不允许此操作");
        }

        boolean skipped = dto != null && DeliveryConstant.STEP_STATUS_SKIPPED.equals(dto.getStatus());
        LocalDateTime now = LocalDateTime.now();
        step.setStatus(skipped ? DeliveryConstant.STEP_STATUS_SKIPPED : DeliveryConstant.STEP_STATUS_COMPLETED);
        step.setCompletedTime(now);
        step.setDuration(calculateDurationSeconds(workOrder.getCheckinTime(), now));
        if (dto != null && StringUtils.hasText(dto.getRemark())) {
            step.setRemark(dto.getRemark());
        }
        workOrderStepMapper.updateById(step);

        // 工单状态推进：CHECKED_IN → IN_PROGRESS（第一个步骤完成后）
        if (DeliveryConstant.WORK_ORDER_STATUS_CHECKED_IN.equals(workOrder.getStatus())) {
            WorkOrderEntity update = new WorkOrderEntity();
            update.setId(workOrder.getId());
            update.setVersion(workOrder.getVersion());
            update.setStatus(DeliveryConstant.WORK_ORDER_STATUS_IN_PROGRESS);
            workOrderMapper.updateById(update);
        }

        // 检查是否所有步骤完成
        int[] progress = calculateProgress(workOrderId);
        log.info("[WorkOrderStep] 步骤完成: workOrderId={}, stepId={}, progress={}/{}",
                workOrderId, stepId, progress[0], progress[1]);
        return workOrderStepMapper.selectByWorkOrderId(workOrderId).stream()
                .filter(s -> stepId.equals(s.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public int[] calculateProgress(Long workOrderId) {
        List<WorkOrderStepEntity> steps = workOrderStepMapper.selectEntitiesByWorkOrderId(workOrderId);
        int total = steps.size();
        int completed = 0;
        for (WorkOrderStepEntity s : steps) {
            if (DeliveryConstant.STEP_STATUS_COMPLETED.equals(s.getStatus())
                    || DeliveryConstant.STEP_STATUS_SKIPPED.equals(s.getStatus())) {
                completed++;
            }
        }
        return new int[]{completed, total};
    }

    /**
     * 计算耗时秒数（相对签到时间）
     */
    private Integer calculateDurationSeconds(LocalDateTime checkinTime, LocalDateTime completedTime) {
        if (checkinTime == null || completedTime == null) {
            return null;
        }
        long seconds = Duration.between(checkinTime, completedTime).getSeconds();
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }
}
