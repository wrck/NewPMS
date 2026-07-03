package com.vibe.delivery.service;

import com.vibe.delivery.dto.WorkOrderIssueProcessDTO;
import com.vibe.delivery.dto.WorkOrderIssueReportDTO;
import com.vibe.delivery.vo.WorkOrderIssueVO;

import java.util.List;

/**
 * 工单异常问题服务
 *
 * @author vibe
 */
public interface WorkOrderIssueService {

    /**
     * 上报异常问题：
     * <ul>
     *   <li>保存问题记录（类型/严重程度/描述/照片）</li>
     *   <li>状态初始化为 OPEN</li>
     *   <li>自动通知 PM（Phase 1 记录日志，Phase 2 接通知引擎）</li>
     * </ul>
     *
     * @return 问题ID
     */
    Long reportIssue(Long workOrderId, WorkOrderIssueReportDTO dto);

    /**
     * 查询工单的异常问题列表
     */
    List<WorkOrderIssueVO> listByWorkOrder(Long workOrderId);

    /**
     * 异常问题详情
     */
    WorkOrderIssueVO getDetail(Long issueId);

    /**
     * 处理异常问题（状态流转 OPEN → PROCESSING → RESOLVED → CLOSED）
     *
     * @return 更新后的问题视图
     */
    WorkOrderIssueVO process(Long issueId, WorkOrderIssueProcessDTO dto);
}
