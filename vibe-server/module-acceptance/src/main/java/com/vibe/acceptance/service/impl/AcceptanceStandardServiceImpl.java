package com.vibe.acceptance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.acceptance.dto.AcceptanceStandardQueryDTO;
import com.vibe.acceptance.dto.AcceptanceStandardSaveDTO;
import com.vibe.acceptance.entity.AcceptanceStandardEntity;
import com.vibe.acceptance.entity.AcceptanceStandardItemEntity;
import com.vibe.acceptance.mapper.AcceptanceStandardItemMapper;
import com.vibe.acceptance.mapper.AcceptanceStandardMapper;
import com.vibe.acceptance.service.AcceptanceStandardService;
import com.vibe.acceptance.vo.AcceptanceStandardVO;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 验收标准 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class AcceptanceStandardServiceImpl implements AcceptanceStandardService {

    private final AcceptanceStandardMapper standardMapper;
    private final AcceptanceStandardItemMapper itemMapper;

    @Override
    public PageResult<AcceptanceStandardVO> page(AcceptanceStandardQueryDTO query) {
        LambdaQueryWrapper<AcceptanceStandardEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getName() != null && !query.getName().isBlank()) {
            wrapper.like(AcceptanceStandardEntity::getName, query.getName());
        }
        if (query.getProjectType() != null && !query.getProjectType().isBlank()) {
            wrapper.eq(AcceptanceStandardEntity::getProjectType, query.getProjectType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AcceptanceStandardEntity::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(AcceptanceStandardEntity::getCreateTime);

        Page<AcceptanceStandardEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<AcceptanceStandardEntity> result = standardMapper.selectPage(page, wrapper);

        List<AcceptanceStandardVO> records = new ArrayList<>();
        for (AcceptanceStandardEntity e : result.getRecords()) {
            AcceptanceStandardVO vo = new AcceptanceStandardVO();
            BeanUtils.copyProperties(e, vo);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public List<AcceptanceStandardVO> listEnabled() {
        LambdaQueryWrapper<AcceptanceStandardEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AcceptanceStandardEntity::getStatus, 1)
               .orderByDesc(AcceptanceStandardEntity::getCreateTime);
        List<AcceptanceStandardEntity> list = standardMapper.selectList(wrapper);
        List<AcceptanceStandardVO> result = new ArrayList<>(list.size());
        for (AcceptanceStandardEntity e : list) {
            AcceptanceStandardVO vo = new AcceptanceStandardVO();
            BeanUtils.copyProperties(e, vo);
            result.add(vo);
        }
        return result;
    }

    @Override
    public AcceptanceStandardVO getDetail(Long id) {
        AcceptanceStandardEntity entity = standardMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("验收标准");
        }
        AcceptanceStandardVO vo = new AcceptanceStandardVO();
        BeanUtils.copyProperties(entity, vo);
        // 加载检查项
        LambdaQueryWrapper<AcceptanceStandardItemEntity> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(AcceptanceStandardItemEntity::getStandardId, id)
                   .orderByAsc(AcceptanceStandardItemEntity::getSortOrder);
        List<AcceptanceStandardItemEntity> items = itemMapper.selectList(itemWrapper);
        List<AcceptanceStandardVO.ItemVO> itemVOs = new ArrayList<>(items.size());
        for (AcceptanceStandardItemEntity item : items) {
            AcceptanceStandardVO.ItemVO itemVO = new AcceptanceStandardVO.ItemVO();
            BeanUtils.copyProperties(item, itemVO);
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(AcceptanceStandardSaveDTO dto) {
        AcceptanceStandardEntity entity = new AcceptanceStandardEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getStandardVersion() == null || entity.getStandardVersion().isBlank()) {
            entity.setStandardVersion("1.0.0");
        }
        standardMapper.insert(entity);
        // 保存检查项
        saveItems(entity.getId(), dto.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AcceptanceStandardSaveDTO dto) {
        AcceptanceStandardEntity entity = standardMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("验收标准");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        standardMapper.updateById(entity);
        // 全量替换检查项：先删后插
        itemMapper.delete(new LambdaQueryWrapper<AcceptanceStandardItemEntity>()
                .eq(AcceptanceStandardItemEntity::getStandardId, id));
        saveItems(id, dto.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AcceptanceStandardEntity entity = standardMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("验收标准");
        }
        standardMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<AcceptanceStandardItemEntity>()
                .eq(AcceptanceStandardItemEntity::getStandardId, id));
    }

    private void saveItems(Long standardId, List<AcceptanceStandardSaveDTO.ItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (AcceptanceStandardSaveDTO.ItemDTO item : items) {
            AcceptanceStandardItemEntity itemEntity = new AcceptanceStandardItemEntity();
            BeanUtils.copyProperties(item, itemEntity);
            itemEntity.setStandardId(standardId);
            if (itemEntity.getSortOrder() == null) {
                itemEntity.setSortOrder(0);
            }
            itemMapper.insert(itemEntity);
        }
    }
}
