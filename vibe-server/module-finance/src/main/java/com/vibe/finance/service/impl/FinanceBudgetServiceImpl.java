package com.vibe.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.finance.constant.FinanceConstant;
import com.vibe.finance.dto.FinanceBudgetQueryDTO;
import com.vibe.finance.dto.FinanceBudgetSaveDTO;
import com.vibe.finance.entity.FinanceBudgetEntity;
import com.vibe.finance.entity.FinanceCostEntity;
import com.vibe.finance.mapper.FinanceBudgetMapper;
import com.vibe.finance.mapper.FinanceCostMapper;
import com.vibe.finance.service.FinanceBudgetService;
import com.vibe.finance.vo.FinanceBudgetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目预算 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class FinanceBudgetServiceImpl implements FinanceBudgetService {

    private final FinanceBudgetMapper budgetMapper;
    private final FinanceCostMapper costMapper;

    @Override
    public PageResult<FinanceBudgetVO> page(FinanceBudgetQueryDTO query) {
        LambdaQueryWrapper<FinanceBudgetEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getProjectId() != null) {
            wrapper.eq(FinanceBudgetEntity::getProjectId, query.getProjectId());
        }
        if (query.getYear() != null) {
            wrapper.eq(FinanceBudgetEntity::getYear, query.getYear());
        }
        if (query.getApprovalStatus() != null && !query.getApprovalStatus().isBlank()) {
            wrapper.eq(FinanceBudgetEntity::getApprovalStatus, query.getApprovalStatus());
        }
        wrapper.orderByDesc(FinanceBudgetEntity::getCreateTime);

        Page<FinanceBudgetEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<FinanceBudgetEntity> result = budgetMapper.selectPage(page, wrapper);

        List<FinanceBudgetVO> records = new ArrayList<>();
        for (FinanceBudgetEntity e : result.getRecords()) {
            records.add(toVO(e));
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public FinanceBudgetVO getDetail(Long id) {
        FinanceBudgetEntity entity = budgetMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("项目预算");
        }
        FinanceBudgetVO vo = toVO(entity);
        // 填充实际成本
        vo.setActualLabor(sumActualCost(entity.getProjectId(), entity.getYear(), FinanceConstant.COST_TYPE_LABOR));
        vo.setActualTravel(sumActualCost(entity.getProjectId(), entity.getYear(), FinanceConstant.COST_TYPE_TRAVEL));
        vo.setActualAgent(sumActualCost(entity.getProjectId(), entity.getYear(), FinanceConstant.COST_TYPE_AGENT));
        vo.setActualOther(sumActualCost(entity.getProjectId(), entity.getYear(), FinanceConstant.COST_TYPE_OTHER));
        BigDecimal actualTotal = nvl(vo.getActualLabor())
                .add(nvl(vo.getActualTravel()))
                .add(nvl(vo.getActualAgent()))
                .add(nvl(vo.getActualOther()));
        vo.setActualTotal(actualTotal);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(FinanceBudgetSaveDTO dto) {
        FinanceBudgetEntity entity = new FinanceBudgetEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setTotalAmount(calcTotal(dto.getLaborAmount(), dto.getTravelAmount(),
                dto.getAgentAmount(), dto.getOtherAmount()));
        entity.setApprovalStatus(FinanceConstant.BUDGET_STATUS_DRAFT);
        budgetMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinanceBudgetSaveDTO dto) {
        FinanceBudgetEntity entity = budgetMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("项目预算");
        }
        if (!FinanceConstant.BUDGET_STATUS_DRAFT.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的预算可修改");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        entity.setTotalAmount(calcTotal(dto.getLaborAmount(), dto.getTravelAmount(),
                dto.getAgentAmount(), dto.getOtherAmount()));
        budgetMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FinanceBudgetEntity entity = budgetMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("项目预算");
        }
        budgetMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        FinanceBudgetEntity entity = budgetMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("项目预算");
        }
        if (!FinanceConstant.BUDGET_STATUS_DRAFT.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的预算可提交审批");
        }
        entity.setApprovalStatus(FinanceConstant.BUDGET_STATUS_PENDING);
        budgetMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, boolean passed, String remark) {
        FinanceBudgetEntity entity = budgetMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("项目预算");
        }
        if (!FinanceConstant.BUDGET_STATUS_PENDING.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅待审批状态的预算可审批");
        }
        entity.setApprovalStatus(passed
                ? FinanceConstant.BUDGET_STATUS_APPROVED
                : FinanceConstant.BUDGET_STATUS_REJECTED);
        entity.setApproverId(UserContextHolder.getUserId());
        entity.setApproveTime(LocalDateTime.now());
        if (remark != null) {
            entity.setRemark(remark);
        }
        budgetMapper.updateById(entity);
    }

    @Override
    public BigDecimal sumActualCost(Long projectId, Integer year, String costType) {
        LambdaQueryWrapper<FinanceCostEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinanceCostEntity::getProjectId, projectId);
        if (year != null) {
            // 按年度过滤：cost_date 在 [year-01-01, year-12-31]
            wrapper.ge(FinanceCostEntity::getCostDate, java.time.LocalDate.of(year, 1, 1));
            wrapper.le(FinanceCostEntity::getCostDate, java.time.LocalDate.of(year, 12, 31));
        }
        if (costType != null && !costType.isBlank()) {
            wrapper.eq(FinanceCostEntity::getCostType, costType);
        }
        List<FinanceCostEntity> list = costMapper.selectList(wrapper);
        return list.stream().map(FinanceCostEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /* ============ 私有方法 ============ */

    private FinanceBudgetVO toVO(FinanceBudgetEntity e) {
        FinanceBudgetVO vo = new FinanceBudgetVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }

    private BigDecimal calcTotal(BigDecimal... amounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal a : amounts) {
            if (a != null) {
                total = total.add(a);
            }
        }
        return total;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
