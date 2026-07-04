package com.vibe.finance.service;

import com.vibe.common.result.PageResult;
import com.vibe.finance.dto.FinanceWorkloadQueryDTO;
import com.vibe.finance.dto.FinanceWorkloadSaveDTO;
import com.vibe.finance.vo.FinanceWorkloadConfirmationVO;

/**
 * 代理商结算 Service
 *
 * @author vibe
 */
public interface FinanceWorkloadService {

    /**
     * 分页查询结算单
     */
    PageResult<FinanceWorkloadConfirmationVO> page(FinanceWorkloadQueryDTO query);

    /**
     * 结算单详情
     */
    FinanceWorkloadConfirmationVO getDetail(Long id);

    /**
     * 创建结算单
     */
    Long save(FinanceWorkloadSaveDTO dto);

    /**
     * 更新结算单（仅 DRAFT 可改）
     */
    void update(Long id, FinanceWorkloadSaveDTO dto);

    /**
     * 删除结算单
     */
    void delete(Long id);

    /**
     * PM 确认工作量
     */
    void pmConfirm(Long id);

    /**
     * 代理商确认工作量
     */
    void agentConfirm(Long id);

    /**
     * 总监审批
     */
    void directorApprove(Long id, boolean passed, String remark);

    /**
     * 财务审批
     */
    void financeApprove(Long id, boolean passed, String remark);

    /**
     * 更新付款状态
     */
    void updatePaymentStatus(Long id, String paymentStatus);
}
