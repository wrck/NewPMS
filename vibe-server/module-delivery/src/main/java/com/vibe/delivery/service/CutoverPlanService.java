package com.vibe.delivery.service;

import com.vibe.common.result.PageResult;
import com.vibe.delivery.dto.CutoverApprovalDTO;
import com.vibe.delivery.dto.CutoverCompleteDTO;
import com.vibe.delivery.dto.CutoverPlanCreateDTO;
import com.vibe.delivery.dto.CutoverPlanQueryDTO;
import com.vibe.delivery.dto.CutoverStepExecuteDTO;
import com.vibe.delivery.vo.CutoverExecutionLogVO;
import com.vibe.delivery.vo.CutoverPlanDetailVO;
import com.vibe.delivery.vo.CutoverPlanVO;

import java.util.List;

/**
 * 割接方案 Service
 *
 * <p>覆盖割接管理全流程：
 * 编制 → 内部审批 → 客户审批 → 执行 → 完成/中止。</p>
 *
 * @author vibe
 */
public interface CutoverPlanService {

    /* ============ 基础 CRUD ============ */

    /**
     * 分页查询割接方案
     */
    PageResult<CutoverPlanVO> page(CutoverPlanQueryDTO query);

    /**
     * 获取割接方案详情（含步骤、操作日志）
     */
    CutoverPlanDetailVO getDetail(Long id);

    /**
     * 创建割接方案（含步骤）
     */
    Long create(CutoverPlanCreateDTO dto);

    /**
     * 更新割接方案（仅 DRAFT 可改，含步骤）
     */
    void update(Long id, CutoverPlanCreateDTO dto);

    /**
     * 删除割接方案（仅 DRAFT 可删）
     */
    void delete(Long id);

    /* ============ 审批流程 ============ */

    /**
     * 提交内部审批（DRAFT → PENDING_INTERNAL_APPROVAL）
     */
    void submitInternalApproval(Long planId);

    /**
     * 内部审批通过（PENDING_INTERNAL_APPROVAL → INTERNAL_APPROVED）
     */
    void internalApprove(CutoverApprovalDTO dto);

    /**
     * 内部审批驳回（PENDING_INTERNAL_APPROVAL → INTERNAL_REJECTED）
     */
    void internalReject(CutoverApprovalDTO dto);

    /**
     * 发起客户审批（INTERNAL_APPROVED → PENDING_CUSTOMER_APPROVAL，生成客户签核链接）
     */
    String startCustomerApproval(Long planId);

    /**
     * 客户审批通过（PENDING_CUSTOMER_APPROVAL → CUSTOMER_APPROVED）
     */
    void customerApprove(CutoverApprovalDTO dto);

    /**
     * 客户审批驳回（PENDING_CUSTOMER_APPROVAL → CUSTOMER_REJECTED）
     */
    void customerReject(CutoverApprovalDTO dto);

    /* ============ 执行流程 ============ */

    /**
     * 开始执行（CUSTOMER_APPROVED → EXECUTING）
     */
    void startExecution(Long planId);

    /**
     * 执行步骤（步骤 PENDING → EXECUTING → COMPLETED）
     *
     * <p>首次调用：步骤 PENDING → EXECUTING（记录实际开始时间）。
     * 再次调用：步骤 EXECUTING → COMPLETED（记录实际结束时间+耗时）。</p>
     */
    void executeStep(CutoverStepExecuteDTO dto);

    /**
     * 回退步骤（步骤 EXECUTING → ROLLED_BACK，执行回退方案）
     */
    void rollbackStep(CutoverStepExecuteDTO dto);

    /**
     * 步骤异常（步骤 EXECUTING → ABORTED，记录异常信息）
     */
    void exceptionStep(CutoverStepExecuteDTO dto);

    /**
     * 完成割接（EXECUTING → COMPLETED，所有步骤需已完成）
     */
    void complete(CutoverCompleteDTO dto);

    /**
     * 中止割接（任意状态 → ABORTED，紧急情况使用）
     */
    void abort(Long planId, String remark);

    /* ============ 查询 ============ */

    /**
     * 查询割接方案的操作日志
     */
    List<CutoverExecutionLogVO> listLogs(Long planId);
}
