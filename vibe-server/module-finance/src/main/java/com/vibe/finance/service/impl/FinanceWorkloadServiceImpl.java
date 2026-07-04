package com.vibe.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.finance.constant.FinanceConstant;
import com.vibe.finance.dto.FinanceWorkloadQueryDTO;
import com.vibe.finance.dto.FinanceWorkloadSaveDTO;
import com.vibe.finance.entity.FinanceWorkloadConfirmationEntity;
import com.vibe.finance.mapper.FinanceWorkloadConfirmationMapper;
import com.vibe.finance.service.FinanceWorkloadService;
import com.vibe.finance.vo.FinanceWorkloadConfirmationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 代理商结算 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class FinanceWorkloadServiceImpl implements FinanceWorkloadService {

    private final FinanceWorkloadConfirmationMapper workloadMapper;

    @Override
    public PageResult<FinanceWorkloadConfirmationVO> page(FinanceWorkloadQueryDTO query) {
        LambdaQueryWrapper<FinanceWorkloadConfirmationEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getProjectId() != null) {
            wrapper.eq(FinanceWorkloadConfirmationEntity::getProjectId, query.getProjectId());
        }
        if (query.getAgentCompanyId() != null) {
            wrapper.eq(FinanceWorkloadConfirmationEntity::getAgentCompanyId, query.getAgentCompanyId());
        }
        if (query.getPeriod() != null && !query.getPeriod().isBlank()) {
            wrapper.eq(FinanceWorkloadConfirmationEntity::getPeriod, query.getPeriod());
        }
        if (query.getApprovalStatus() != null && !query.getApprovalStatus().isBlank()) {
            wrapper.eq(FinanceWorkloadConfirmationEntity::getApprovalStatus, query.getApprovalStatus());
        }
        if (query.getPaymentStatus() != null && !query.getPaymentStatus().isBlank()) {
            wrapper.eq(FinanceWorkloadConfirmationEntity::getPaymentStatus, query.getPaymentStatus());
        }
        wrapper.orderByDesc(FinanceWorkloadConfirmationEntity::getCreateTime);

        Page<FinanceWorkloadConfirmationEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<FinanceWorkloadConfirmationEntity> result = workloadMapper.selectPage(page, wrapper);

        List<FinanceWorkloadConfirmationVO> records = new ArrayList<>();
        for (FinanceWorkloadConfirmationEntity e : result.getRecords()) {
            records.add(toVO(e));
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public FinanceWorkloadConfirmationVO getDetail(Long id) {
        FinanceWorkloadConfirmationEntity entity = workloadMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("结算单");
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(FinanceWorkloadSaveDTO dto) {
        FinanceWorkloadConfirmationEntity entity = new FinanceWorkloadConfirmationEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setTotalAmount(calcTotal(dto.getWorkloadDays(), dto.getUnitPrice(),
                dto.getTravelAmount(), dto.getOtherAmount()));
        entity.setApprovalStatus(FinanceConstant.SETTLEMENT_STATUS_DRAFT);
        entity.setPaymentStatus(FinanceConstant.PAYMENT_STATUS_UNPAID);
        workloadMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinanceWorkloadSaveDTO dto) {
        FinanceWorkloadConfirmationEntity entity = workloadMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("结算单");
        }
        if (!FinanceConstant.SETTLEMENT_STATUS_DRAFT.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的结算单可修改");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        entity.setTotalAmount(calcTotal(dto.getWorkloadDays(), dto.getUnitPrice(),
                dto.getTravelAmount(), dto.getOtherAmount()));
        workloadMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FinanceWorkloadConfirmationEntity entity = workloadMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("结算单");
        }
        workloadMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pmConfirm(Long id) {
        FinanceWorkloadConfirmationEntity entity = workloadMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("结算单");
        }
        if (!FinanceConstant.SETTLEMENT_STATUS_DRAFT.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的结算单可由PM确认");
        }
        entity.setApprovalStatus(FinanceConstant.SETTLEMENT_STATUS_PM_CONFIRMED);
        entity.setPmConfirmUserId(UserContextHolder.getUserId());
        entity.setPmConfirmTime(LocalDateTime.now());
        workloadMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agentConfirm(Long id) {
        FinanceWorkloadConfirmationEntity entity = workloadMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("结算单");
        }
        if (!FinanceConstant.SETTLEMENT_STATUS_PM_CONFIRMED.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅PM已确认的结算单可由代理商确认");
        }
        entity.setApprovalStatus(FinanceConstant.SETTLEMENT_STATUS_AGENT_CONFIRMED);
        // 代理商确认后进入待审批
        entity.setApprovalStatus(FinanceConstant.SETTLEMENT_STATUS_PENDING);
        entity.setAgentConfirmUserId(UserContextHolder.getUserId());
        entity.setAgentConfirmTime(LocalDateTime.now());
        workloadMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void directorApprove(Long id, boolean passed, String remark) {
        FinanceWorkloadConfirmationEntity entity = workloadMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("结算单");
        }
        if (!FinanceConstant.SETTLEMENT_STATUS_PENDING.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅待审批状态的结算单可由总监审批");
        }
        entity.setApprovalStatus(passed
                ? FinanceConstant.SETTLEMENT_STATUS_DIRECTOR_APPROVED
                : FinanceConstant.SETTLEMENT_STATUS_REJECTED);
        if (remark != null) {
            entity.setRemark(remark);
        }
        workloadMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void financeApprove(Long id, boolean passed, String remark) {
        FinanceWorkloadConfirmationEntity entity = workloadMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("结算单");
        }
        if (!FinanceConstant.SETTLEMENT_STATUS_DIRECTOR_APPROVED.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅总监已审批的结算单可由财务审批");
        }
        entity.setApprovalStatus(passed
                ? FinanceConstant.SETTLEMENT_STATUS_FINANCE_APPROVED
                : FinanceConstant.SETTLEMENT_STATUS_REJECTED);
        if (remark != null) {
            entity.setRemark(remark);
        }
        workloadMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentStatus(Long id, String paymentStatus) {
        FinanceWorkloadConfirmationEntity entity = workloadMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("结算单");
        }
        if (!FinanceConstant.SETTLEMENT_STATUS_FINANCE_APPROVED.equals(entity.getApprovalStatus())) {
            throw BusinessException.stateNotAllowed("仅财务已审批的结算单可更新付款状态");
        }
        entity.setPaymentStatus(paymentStatus);
        workloadMapper.updateById(entity);
    }

    /* ============ 私有方法 ============ */

    private FinanceWorkloadConfirmationVO toVO(FinanceWorkloadConfirmationEntity e) {
        FinanceWorkloadConfirmationVO vo = new FinanceWorkloadConfirmationVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }

    /**
     * 结算总额 = 工作量 × 单价 + 差旅 + 其他
     */
    private BigDecimal calcTotal(BigDecimal workloadDays, BigDecimal unitPrice,
                                  BigDecimal travelAmount, BigDecimal otherAmount) {
        BigDecimal labor = nvl(workloadDays).multiply(nvl(unitPrice));
        return labor.add(nvl(travelAmount)).add(nvl(otherAmount));
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
