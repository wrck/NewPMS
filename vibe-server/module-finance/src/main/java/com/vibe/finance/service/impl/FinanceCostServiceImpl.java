package com.vibe.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.finance.dto.FinanceCostQueryDTO;
import com.vibe.finance.dto.FinanceCostSaveDTO;
import com.vibe.finance.entity.FinanceCostEntity;
import com.vibe.finance.mapper.FinanceCostMapper;
import com.vibe.finance.service.FinanceCostService;
import com.vibe.finance.vo.FinanceCostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 成本归集 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class FinanceCostServiceImpl implements FinanceCostService {

    private final FinanceCostMapper costMapper;

    @Override
    public PageResult<FinanceCostVO> page(FinanceCostQueryDTO query) {
        LambdaQueryWrapper<FinanceCostEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getProjectId() != null) {
            wrapper.eq(FinanceCostEntity::getProjectId, query.getProjectId());
        }
        if (query.getCostType() != null && !query.getCostType().isBlank()) {
            wrapper.eq(FinanceCostEntity::getCostType, query.getCostType());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(FinanceCostEntity::getCostDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(FinanceCostEntity::getCostDate, query.getEndDate());
        }
        wrapper.orderByDesc(FinanceCostEntity::getCostDate);

        Page<FinanceCostEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<FinanceCostEntity> result = costMapper.selectPage(page, wrapper);

        List<FinanceCostVO> records = new ArrayList<>();
        for (FinanceCostEntity e : result.getRecords()) {
            FinanceCostVO vo = new FinanceCostVO();
            BeanUtils.copyProperties(e, vo);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public FinanceCostVO getDetail(Long id) {
        FinanceCostEntity entity = costMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("成本记录");
        }
        FinanceCostVO vo = new FinanceCostVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(FinanceCostSaveDTO dto) {
        FinanceCostEntity entity = new FinanceCostEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getRefType() == null || entity.getRefType().isBlank()) {
            entity.setRefType("MANUAL");
        }
        costMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinanceCostSaveDTO dto) {
        FinanceCostEntity entity = costMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("成本记录");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        costMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FinanceCostEntity entity = costMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("成本记录");
        }
        costMapper.deleteById(id);
    }
}
