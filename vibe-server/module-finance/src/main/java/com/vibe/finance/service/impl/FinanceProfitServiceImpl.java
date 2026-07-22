package com.vibe.finance.service.impl;

import com.vibe.finance.mapper.FinanceCostMapper;
import com.vibe.finance.service.FinanceProfitService;
import com.vibe.finance.vo.FinanceProfitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * 利润分析 Service 实现
 *
 * <p>注：本实现基于 finance_cost 表的成本数据计算。收入字段暂时返回 0（待项目表补充合同金额字段后完善）。
 * 项目名称通过 finance_cost JOIN project 表直接获取（跨模块 SQL JOIN，参考 OutsourceTaskMapper 模式）。</p>
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class FinanceProfitServiceImpl implements FinanceProfitService {

    private final FinanceCostMapper costMapper;

    @Override
    public FinanceProfitVO getProjectProfit(Long projectId) {
        FinanceProfitVO vo = costMapper.selectProjectProfit(projectId);
        if (vo == null) {
            vo = new FinanceProfitVO();
            vo.setProjectId(projectId);
        }
        enrichVO(vo);
        return vo;
    }

    @Override
    public List<FinanceProfitVO> listProjectProfit() {
        List<FinanceProfitVO> result = costMapper.selectProjectProfitList();
        for (FinanceProfitVO vo : result) {
            enrichVO(vo);
        }
        // 按利润倒序
        result.sort((a, b) -> nvl(b.getProfit()).compareTo(nvl(a.getProfit())));
        return result;
    }

    @Override
    public List<FinanceProfitVO> listProfitByCustomer() {
        // TODO: 待项目表补充客户字段后实现按客户维度聚合
        return Collections.emptyList();
    }

    @Override
    public List<FinanceProfitVO> listProfitByRegion() {
        // TODO: 待项目表补充区域字段后实现按区域维度聚合
        return Collections.emptyList();
    }

    @Override
    public List<FinanceProfitVO> listProfitByProductLine() {
        // TODO: 待项目表补充产品线字段后实现按产品线维度聚合
        return Collections.emptyList();
    }

    /* ============ 私有方法 ============ */

    /**
     * 基于 mapper 聚合出的 selfCost / agentCost，计算 totalCost / 收入 / 利润 / 各项比率。
     * 收入暂返回 0（待项目表补充合同金额字段）。
     */
    private void enrichVO(FinanceProfitVO vo) {
        BigDecimal selfCost = nvl(vo.getSelfCost());
        BigDecimal agentCost = nvl(vo.getAgentCost());
        BigDecimal totalCost = selfCost.add(agentCost);

        // 收入暂返回 0（待项目表补充合同金额字段）
        BigDecimal revenue = BigDecimal.ZERO;

        BigDecimal profit = revenue.subtract(totalCost);
        BigDecimal profitMargin = revenue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profit.multiply(new BigDecimal("100")).divide(revenue, 2, RoundingMode.HALF_UP);
        BigDecimal selfCostRatio = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : selfCost.multiply(new BigDecimal("100")).divide(totalCost, 2, RoundingMode.HALF_UP);
        BigDecimal agentCostRatio = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : agentCost.multiply(new BigDecimal("100")).divide(totalCost, 2, RoundingMode.HALF_UP);

        vo.setRevenue(revenue);
        vo.setSelfCost(selfCost);
        vo.setAgentCost(agentCost);
        vo.setTotalCost(totalCost);
        vo.setProfit(profit);
        vo.setProfitMargin(profitMargin);
        vo.setSelfCostRatio(selfCostRatio);
        vo.setAgentCostRatio(agentCostRatio);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
