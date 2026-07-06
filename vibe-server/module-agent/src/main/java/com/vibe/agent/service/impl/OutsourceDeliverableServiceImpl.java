package com.vibe.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.DeliverableReviewDTO;
import com.vibe.agent.dto.OutsourceDeliverableDTO;
import com.vibe.agent.entity.OutsourceDeliverableEntity;
import com.vibe.agent.entity.OutsourceTaskEntity;
import com.vibe.agent.mapper.OutsourceDeliverableMapper;
import com.vibe.agent.mapper.OutsourceTaskMapper;
import com.vibe.agent.service.OutsourceDeliverableService;
import com.vibe.agent.service.OutsourceTaskService;
import com.vibe.agent.vo.OutsourceDeliverableVO;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.event.DomainEventPublisher;
import com.vibe.event.events.DeliverableReviewedEvent;
import com.vibe.event.events.DeliverableSubmittedEvent;
import com.vibe.system.notification.NotificationConstant;
import com.vibe.system.notification.NotificationEvent;
import com.vibe.system.notification.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理商交付物服务实现
 *
 * <p>交付物必传校验：施工照片≥{@link AgentConstant#MIN_PHOTO_COUNT}张、测试记录必传、签收单必传。
 * 校验在 DTO 层（@NotEmpty）+ Service 层（数量校验）双重保障。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutsourceDeliverableServiceImpl implements OutsourceDeliverableService {

    private final OutsourceDeliverableMapper deliverableMapper;
    private final OutsourceTaskMapper outsourceTaskMapper;
    private final OutsourceTaskService outsourceTaskService;
    private final NotificationProducer notificationProducer;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submit(OutsourceDeliverableDTO dto) {
        // 校验转包任务存在
        OutsourceTaskEntity task = outsourceTaskMapper.selectById(dto.getOutsourceTaskId());
        if (task == null) {
            throw BusinessException.of(ResultCode.OUTSOURCE_TASK_NOT_FOUND);
        }

        // 校验施工照片数量 ≥ MIN_PHOTO_COUNT
        if (CollectionUtils.isEmpty(dto.getPhotos())
                || dto.getPhotos().size() < AgentConstant.MIN_PHOTO_COUNT) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                    "施工照片至少上传 " + AgentConstant.MIN_PHOTO_COUNT + " 张");
        }

        // 保存交付物
        int count = 0;
        count += saveDeliverables(dto.getOutsourceTaskId(), AgentConstant.DELIVERABLE_PHOTO, dto.getPhotos());
        count += saveDeliverables(dto.getOutsourceTaskId(), AgentConstant.DELIVERABLE_TEST_RECORD, dto.getTestRecords());
        count += saveDeliverables(dto.getOutsourceTaskId(), AgentConstant.DELIVERABLE_RECEIPT, dto.getReceipts());
        if (!CollectionUtils.isEmpty(dto.getConfigs())) {
            count += saveDeliverables(dto.getOutsourceTaskId(), AgentConstant.DELIVERABLE_CONFIG, dto.getConfigs());
        }

        // 保存完成情况描述作为 OTHER 类型交付物（remark 存储）
        if (dto.getCompletionDescription() != null) {
            OutsourceDeliverableEntity descEntity = new OutsourceDeliverableEntity();
            descEntity.setOutsourceTaskId(dto.getOutsourceTaskId());
            descEntity.setDeliverableType(AgentConstant.DELIVERABLE_OTHER);
            descEntity.setRemark(dto.getCompletionDescription());
            deliverableMapper.insert(descEntity);
            count++;
        }

        // 触发任务状态流转：IN_PROGRESS/RETURNED → SUBMITTED
        outsourceTaskService.markSubmitted(dto.getOutsourceTaskId());

        log.info("代理商提交交付物: taskId={}, count={}", dto.getOutsourceTaskId(), count);

        // 通知事件投递：DELIVERABLE_REVIEW（通知 PM 审核交付物）
        sendDeliverableReviewNotification(task, count);

        // 发布交付物提交领域事件
        // 业务约定：deliverableId 用 outsourceTaskId 代表本批次提交，deliverableType 固定为 OUTSOURCE_DELIVERABLE
        domainEventPublisher.publish(new DeliverableSubmittedEvent(
                dto.getOutsourceTaskId(), task.getProjectId(), task.getAgentEngineerId(),
                task.getTaskScope(), AgentConstant.DELIVERABLE_OTHER));
        return count;
    }

    /**
     * 投递交付物审核通知（通知 PM）。
     *
     * <p>PM 的 userId 跨模块无法直接获取，recipientIds 传空列表，
     * 消费侧会通过飞书群消息触达 PM 群。站内信写入需要 PM userId 时由后续补充。</p>
     */
    private void sendDeliverableReviewNotification(OutsourceTaskEntity task, int submitCount) {
        Map<String, String> variables = new HashMap<>(4);
        variables.put("taskName", task.getTaskScope() == null
                ? "任务#" + task.getId() : task.getTaskScope());
        variables.put("projectName", "");
        variables.put("submitCount", String.valueOf(submitCount));
        NotificationEvent event = NotificationEvent.of(
                NotificationConstant.EVENT_DELIVERABLE_REVIEW,
                NotificationConstant.RECIPIENT_INTERNAL,
                Collections.emptyList(), variables,
                task.getId(), NotificationConstant.BIZ_OUTSOURCE_TASK);
        notificationProducer.send(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long taskId, DeliverableReviewDTO dto) {
        if (dto.getApproved() == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "审核结果不能为空");
        }
        if (Boolean.FALSE.equals(dto.getApproved()) && !org.springframework.util.StringUtils.hasText(dto.getRejectReason())) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "审核退回时必须填写退回原因");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // 审核通过：SUBMITTED → CONFIRMED
            outsourceTaskService.confirm(taskId);
        } else {
            // 审核退回：SUBMITTED → RETURNED
            com.vibe.agent.dto.OutsourceTaskActionDTO actionDTO = new com.vibe.agent.dto.OutsourceTaskActionDTO();
            actionDTO.setReason(dto.getRejectReason());
            outsourceTaskService.returnTask(taskId, actionDTO);
        }
        log.info("PM 审核交付物: taskId={}, approved={}", taskId, dto.getApproved());

        // 通知事件投递：审核通过 → DELIVERABLE_CONFIRMED；审核退回 → DELIVERABLE_RETURNED
        // 查询 task 实体以获取代理商工程师 ID 等信息用于通知
        OutsourceTaskEntity task = outsourceTaskMapper.selectById(taskId);
        if (task != null) {
            sendDeliverableReviewResultNotification(task, dto.getApproved(), dto.getRejectReason());
        }

        // 发布交付物审核领域事件
        String reviewResult = Boolean.TRUE.equals(dto.getApproved()) ? "CONFIRMED" : "RETURNED";
        domainEventPublisher.publish(new DeliverableReviewedEvent(
                taskId, task == null ? null : task.getProjectId(),
                UserContextHolder.getUserId(), reviewResult, dto.getRejectReason()));
    }

    /**
     * 投递交付物审核结果通知（通知代理商工程师）。
     */
    private void sendDeliverableReviewResultNotification(OutsourceTaskEntity task,
                                                         Boolean approved, String rejectReason) {
        Long recipientId = task.getAgentEngineerId();
        if (recipientId == null) {
            log.info("交付物审核结果通知跳过（无代理商工程师）: taskId={}", task.getId());
            return;
        }
        Map<String, String> variables = new HashMap<>(4);
        variables.put("taskName", task.getTaskScope() == null
                ? "任务#" + task.getId() : task.getTaskScope());
        variables.put("projectName", "");
        variables.put("rejectReason", rejectReason == null ? "" : rejectReason);
        String eventType = Boolean.TRUE.equals(approved)
                ? NotificationConstant.EVENT_DELIVERABLE_CONFIRMED
                : NotificationConstant.EVENT_DELIVERABLE_RETURNED;
        NotificationEvent event = NotificationEvent.of(
                eventType, NotificationConstant.RECIPIENT_AGENT,
                Collections.singletonList(recipientId), variables,
                task.getId(), NotificationConstant.BIZ_OUTSOURCE_TASK);
        notificationProducer.send(event);
    }

    @Override
    public List<OutsourceDeliverableVO> listByTaskId(Long taskId) {
        if (taskId == null) {
            return java.util.Collections.emptyList();
        }
        return deliverableMapper.selectByTaskId(taskId);
    }

    @Override
    public Map<String, Integer> countByType(Long taskId) {
        if (taskId == null) {
            return java.util.Collections.emptyMap();
        }
        List<Map<String, Object>> rows = deliverableMapper.countByTaskId(taskId);
        Map<String, Integer> result = new HashMap<>(8);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object type = row.get("deliverableType");
                Object cnt = row.get("cnt");
                if (type != null && cnt != null) {
                    result.put(type.toString(), ((Number) cnt).intValue());
                }
            }
        }
        return result;
    }

    /* ============ 私有方法 ============ */

    /**
     * 批量保存交付物。
     *
     * @return 保存数量
     */
    private int saveDeliverables(Long taskId, String type,
                                 List<OutsourceDeliverableDTO.DeliverableItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return 0;
        }
        int count = 0;
        for (OutsourceDeliverableDTO.DeliverableItem item : items) {
            OutsourceDeliverableEntity entity = new OutsourceDeliverableEntity();
            entity.setOutsourceTaskId(taskId);
            entity.setDeliverableType(type);
            entity.setFileUrl(item.getFileUrl());
            entity.setFileName(item.getFileName());
            entity.setRemark(item.getRemark());
            deliverableMapper.insert(entity);
            count++;
        }
        return count;
    }
}
