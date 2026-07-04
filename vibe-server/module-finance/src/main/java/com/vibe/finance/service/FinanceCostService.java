package com.vibe.finance.service;

import com.vibe.common.result.PageResult;
import com.vibe.finance.dto.FinanceCostQueryDTO;
import com.vibe.finance.dto.FinanceCostSaveDTO;
import com.vibe.finance.vo.FinanceCostVO;

/**
 * 成本归集 Service
 *
 * @author vibe
 */
public interface FinanceCostService {

    /**
     * 分页查询成本
     */
    PageResult<FinanceCostVO> page(FinanceCostQueryDTO query);

    /**
     * 成本详情
     */
    FinanceCostVO getDetail(Long id);

    /**
     * 创建成本
     */
    Long save(FinanceCostSaveDTO dto);

    /**
     * 更新成本
     */
    void update(Long id, FinanceCostSaveDTO dto);

    /**
     * 删除成本
     */
    void delete(Long id);
}
