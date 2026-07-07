package com.vibe.delivery.service.impl;

import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.delivery.constant.DeliveryConstant;
import com.vibe.delivery.dto.WorkOrderIssueProcessDTO;
import com.vibe.delivery.dto.WorkOrderIssueReportDTO;
import com.vibe.delivery.entity.WorkOrderEntity;
import com.vibe.delivery.entity.WorkOrderIssueEntity;
import com.vibe.delivery.mapper.WorkOrderIssueMapper;
import com.vibe.delivery.mapper.WorkOrderMapper;
import com.vibe.delivery.service.WorkOrderIssueService;
import com.vibe.delivery.vo.WorkOrderIssueVO;
import com.vibe.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单异常问题服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderIssueServiceImpl implements WorkOrderIssueService {

    private final WorkOrderIssueMapper workOrderIssueMapper;
    private final WorkOrderMapper workOrderMapper;
    private final MinioUtils minioUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportIssue(Long workOrderId, WorkOrderIssueReportDTO dto) {
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw BusinessException.of(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        // 校验严重程度
        if (!isValidSeverity(dto.getSeverity())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "严重程度取值非法");
        }

        WorkOrderIssueEntity entity = new WorkOrderIssueEntity();
        entity.setWorkOrderId(workOrderId);
        entity.setIssueType(dto.getIssueType());
        entity.setSeverity(dto.getSeverity());
        entity.setDescription(dto.getDescription());
        entity.setPhotos(dto.getPhotoUrls());
        entity.setStatus(DeliveryConstant.ISSUE_STATUS_OPEN);
        entity.setRemark(dto.getRemark());
        workOrderIssueMapper.insert(entity);

        // 自动通知 PM（Phase 1 记录日志，Phase 2 接通知引擎）
        notifyPm(workOrder, entity);

        log.info("[WorkOrderIssue] 异常上报: workOrderId={}, issueId={}, severity={}",
                workOrderId, entity.getId(), dto.getSeverity());
        return entity.getId();
    }

    @Override
    public List<WorkOrderIssueVO> listByWorkOrder(Long workOrderId) {
        List<WorkOrderIssueVO> list = workOrderIssueMapper.selectByWorkOrderId(workOrderId);
        if (list != null) {
            for (WorkOrderIssueVO vo : list) {
                fillPhotoUrls(vo);
            }
        }
        return list;
    }

    @Override
    public WorkOrderIssueVO getDetail(Long issueId) {
        WorkOrderIssueVO vo = workOrderIssueMapper.selectVoById(issueId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "异常问题不存在");
        }
        fillPhotoUrls(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderIssueVO process(Long issueId, WorkOrderIssueProcessDTO dto) {
        WorkOrderIssueEntity entity = workOrderIssueMapper.selectById(issueId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "异常问题不存在");
        }
        String target = dto.getStatus();
        String current = entity.getStatus();
        validateStatusTransition(current, target);

        entity.setStatus(target);
        if (StringUtils.hasText(dto.getRemark())) {
            entity.setRemark(dto.getRemark());
        }
        if (DeliveryConstant.ISSUE_STATUS_RESOLVED.equals(target)
                || DeliveryConstant.ISSUE_STATUS_CLOSED.equals(target)) {
            entity.setResolvedTime(LocalDateTime.now());
        }
        workOrderIssueMapper.updateById(entity);

        log.info("[WorkOrderIssue] 异常处理: issueId={}, {} -> {}, operator={}",
                issueId, current, target, UserContextHolder.getUserId());
        WorkOrderIssueVO vo = workOrderIssueMapper.selectVoById(issueId);
        fillPhotoUrls(vo);
        return vo;
    }

    /* ============ 私有方法 ============ */

    /**
     * 将异常问题照片 objectName 列表转换为 MinIO 预签名 URL 列表
     */
    private void fillPhotoUrls(WorkOrderIssueVO vo) {
        if (vo == null || CollectionUtils.isEmpty(vo.getPhotoUrls())) {
            return;
        }
        List<String> urls = new ArrayList<>(vo.getPhotoUrls().size());
        for (String objectName : vo.getPhotoUrls()) {
            if (StringUtils.hasText(objectName)) {
                try {
                    urls.add(minioUtils.getPresignedDownloadUrl(objectName));
                } catch (Exception e) {
                    log.warn("异常问题照片预签名 URL 生成失败: {}", e.getMessage());
                    urls.add(objectName);
                }
            }
        }
        vo.setPhotoUrls(urls);
    }

    /**
     * 通知 PM：Phase 1 记录日志，Phase 2 通过通知引擎发送飞书/站内信
     */
    private void notifyPm(WorkOrderEntity workOrder, WorkOrderIssueEntity issue) {
        // TODO Phase 2: 投递事件到通知引擎（RabbitMQ），渲染模板 ISSUE_REPORTED
        // 当前 Phase 1 仅记录日志，PM 可通过工单详情查看异常列表
        log.warn("[通知] 异常问题上报通知 PM: workOrderId={}, taskId={}, issueId={}, severity={}, desc={}",
                workOrder.getId(), workOrder.getTaskId(), issue.getId(),
                issue.getSeverity(), issue.getDescription());
    }

    private boolean isValidSeverity(String severity) {
        return DeliveryConstant.ISSUE_SEVERITY_MINOR.equals(severity)
                || DeliveryConstant.ISSUE_SEVERITY_MAJOR.equals(severity)
                || DeliveryConstant.ISSUE_SEVERITY_BLOCKING.equals(severity);
    }

    /**
     * 状态流转校验：OPEN → PROCESSING → RESOLVED → CLOSED
     */
    private void validateStatusTransition(String current, String target) {
        if (!isValidTargetStatus(target)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "目标状态取值非法");
        }
        boolean valid = switch (current) {
            case DeliveryConstant.ISSUE_STATUS_OPEN ->
                    DeliveryConstant.ISSUE_STATUS_PROCESSING.equals(target)
                            || DeliveryConstant.ISSUE_STATUS_RESOLVED.equals(target)
                            || DeliveryConstant.ISSUE_STATUS_CLOSED.equals(target);
            case DeliveryConstant.ISSUE_STATUS_PROCESSING ->
                    DeliveryConstant.ISSUE_STATUS_RESOLVED.equals(target)
                            || DeliveryConstant.ISSUE_STATUS_CLOSED.equals(target);
            case DeliveryConstant.ISSUE_STATUS_RESOLVED ->
                    DeliveryConstant.ISSUE_STATUS_CLOSED.equals(target);
            case DeliveryConstant.ISSUE_STATUS_CLOSED -> false;
            default -> false;
        };
        if (!valid) {
            throw BusinessException.of(ResultCode.STATE_TRANSITION_INVALID,
                    "异常问题状态不允许从 " + current + " 流转到 " + target);
        }
    }

    private boolean isValidTargetStatus(String status) {
        return DeliveryConstant.ISSUE_STATUS_PROCESSING.equals(status)
                || DeliveryConstant.ISSUE_STATUS_RESOLVED.equals(status)
                || DeliveryConstant.ISSUE_STATUS_CLOSED.equals(status);
    }
}
