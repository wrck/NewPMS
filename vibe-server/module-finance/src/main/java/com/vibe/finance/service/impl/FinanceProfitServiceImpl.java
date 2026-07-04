package com.vibe.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.finance.constant.FinanceConstant;
import com.vibe.finance.entity.FinanceCostEntity;
import com.vibe.finance.mapper.FinanceCostMapper;
import com.vibe.finance.service.FinanceProfitService;
import com.vibe.finance.vo.FinanceProfitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 利润分析 Service 实现
 *
 * <p>注：本实现基于 finance_cost 表的成本数据计算。收入字段暂时返回 0（待项目表补充合同金额字段后完善）。</p>
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class FinanceProfitServiceImpl implements FinanceProfitService {

    private final FinanceCostMapper costMapper;

    @Override
    public FinanceProfitVO getProjectProfit(Long projectId) {
        FinanceProfitVO vo = buildProfitVO(projectId, "项目-" + projectId);
        return vo;
    }

    @Override
    public List<FinanceProfitVO> listProjectProfit() {
        // 查询所有成本记录，按 projectId 去重（LambdaQueryWrapper 不支持 distinct()）
        LambdaQueryWrapper<FinanceCostEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(FinanceCostEntity::getProjectId);
        List<FinanceCostEntity> all = costMapper.selectList(wrapper);
        List<Long> projectIds = all.stream()
                .map(FinanceCostEntity::getProjectId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<FinanceProfitVO> result = new ArrayList<>(projectIds.size());
        for (Long pid : projectIds) {
            result.add(buildProfitVO(pid, "项目-" + pid));
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
     * 构建单个项目的利润分析 VO
     */
    private FinanceProfitVO buildProfitVO(Long projectId, String projectName) {
        FinanceProfitVO vo = new FinanceProfitVO();
        vo.setProjectId(projectId);
        vo.setProjectName(projectName);

        // 计算自有成本（LABOR + TRAVEL + OTHER）
        BigDecimal selfCost = sumCostByType(projectId, null)
                .subtract(sumCostByType(projectId, FinanceConstant.COST_TYPE_AGENT));
        // 代理商成本
        BigDecimal agentCost = sumCostByType(projectId, FinanceConstant.COST_TYPE_AGENT);
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
        return vo;
    }

    /**
     * 汇总项目指定成本类型的金额
     */
    private BigDecimal sumCostByType(Long projectId, String costType) {
        LambdaQueryWrapper<FinanceCostEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceCostEntity::getProjectId, projectId);
        if (costType != null && !costType.isBlank()) {
            wrapper.eq(FinanceCostEntity::getCostType, costType);
        }
        List<FinanceCostEntity> list = costMapper.selectList(wrapper);
        return list.stream()
                .map(FinanceCostEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
