package com.vibe.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.finance.entity.FinanceCostEntity;
import com.vibe.finance.vo.FinanceProfitVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成本归集 Mapper
 *
 * @author vibe
 */
@Mapper
public interface FinanceCostMapper extends BaseMapper<FinanceCostEntity> {

    /**
     * 汇总全部项目的利润分析（按 finance_cost 聚合，JOIN project 取真实项目名）
     */
    List<FinanceProfitVO> selectProjectProfitList();

    /**
     * 汇总指定项目的利润分析
     */
    FinanceProfitVO selectProjectProfit(@Param("projectId") Long projectId);
}
