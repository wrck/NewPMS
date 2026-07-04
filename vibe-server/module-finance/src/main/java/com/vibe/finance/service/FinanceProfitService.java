package com.vibe.finance.service;

import com.vibe.finance.vo.FinanceProfitVO;

import java.util.List;

/**
 * 利润分析 Service
 *
 * @author vibe
 */
public interface FinanceProfitService {

    /**
     * 查询指定项目的利润分析
     */
    FinanceProfitVO getProjectProfit(Long projectId);

    /**
     * 查询全部项目利润分析（按利润倒序）
     */
    List<FinanceProfitVO> listProjectProfit();

    /**
     * 按客户维度统计利润
     */
    List<FinanceProfitVO> listProfitByCustomer();

    /**
     * 按区域维度统计利润
     */
    List<FinanceProfitVO> listProfitByRegion();

    /**
     * 按产品线维度统计利润
     */
    List<FinanceProfitVO> listProfitByProductLine();
}
