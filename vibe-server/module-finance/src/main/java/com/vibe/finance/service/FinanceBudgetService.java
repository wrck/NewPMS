package com.vibe.finance.service;

import com.vibe.common.result.PageResult;
import com.vibe.finance.dto.FinanceBudgetQueryDTO;
import com.vibe.finance.dto.FinanceBudgetSaveDTO;
import com.vibe.finance.vo.FinanceBudgetVO;

import java.math.BigDecimal;

/**
 * 项目预算 Service
 *
 * @author vibe
 */
public interface FinanceBudgetService {

    /**
     * 分页查询预算
     */
    PageResult<FinanceBudgetVO> page(FinanceBudgetQueryDTO query);

    /**
     * 预算详情（含实际成本对比）
     */
    FinanceBudgetVO getDetail(Long id);

    /**
     * 创建预算
     */
    Long save(FinanceBudgetSaveDTO dto);

    /**
     * 更新预算（仅 DRAFT 可改）
     */
    void update(Long id, FinanceBudgetSaveDTO dto);

    /**
     * 删除预算
     */
    void delete(Long id);

    /**
     * 提交审批（DRAFT → PENDING）
     */
    void submit(Long id);

    /**
     * 审批通过/驳回（PENDING → APPROVED / REJECTED）
     */
    void approve(Long id, boolean passed, String remark);

    /**
     * 查询项目年度实际成本汇总（按成本类型聚合）
     */
    BigDecimal sumActualCost(Long projectId, Integer year, String costType);
}
