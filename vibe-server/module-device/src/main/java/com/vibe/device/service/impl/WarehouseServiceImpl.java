package com.vibe.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.device.dto.WarehouseDTO;
import com.vibe.device.entity.WarehouseEntity;
import com.vibe.device.mapper.WarehouseMapper;
import com.vibe.device.service.WarehouseService;
import com.vibe.device.vo.WarehouseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 仓库服务实现。
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;

    @Override
    public PageResult<WarehouseVO> page(Integer page, Integer size, String keyword, String region) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : size;
        // 先查总数
        List<WarehouseVO> all = warehouseMapper.selectWarehouseList(keyword, region);
        int total = all.size();
        int from = Math.min((p - 1) * s, total);
        int to = Math.min(from + s, total);
        return PageResult.of(all.subList(from, to), total, p, s);
    }

    @Override
    public List<WarehouseVO> listAll() {
        return warehouseMapper.selectWarehouseList(null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WarehouseDTO dto) {
        checkCodeUnique(dto.getWarehouseCode(), null);
        WarehouseEntity entity = new WarehouseEntity();
        copyDtoToEntity(dto, entity);
        warehouseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WarehouseDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "仓库ID不能为空");
        }
        WarehouseEntity exist = warehouseMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "仓库不存在");
        }
        if (dto.getWarehouseCode() != null && !dto.getWarehouseCode().equals(exist.getWarehouseCode())) {
            checkCodeUnique(dto.getWarehouseCode(), dto.getId());
        }
        copyDtoToEntity(dto, exist);
        warehouseMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (warehouseMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "仓库不存在");
        }
        warehouseMapper.deleteById(id);
    }

    @Override
    public WarehouseVO getDetail(Long id) {
        WarehouseVO vo = warehouseMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "仓库不存在");
        }
        return vo;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<WarehouseEntity> wrapper = new LambdaQueryWrapper<WarehouseEntity>()
                .eq(WarehouseEntity::getWarehouseCode, code);
        if (excludeId != null) {
            wrapper.ne(WarehouseEntity::getId, excludeId);
        }
        if (warehouseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "仓库编码已存在");
        }
    }

    private void copyDtoToEntity(WarehouseDTO dto, WarehouseEntity entity) {
        entity.setWarehouseName(dto.getWarehouseName());
        entity.setWarehouseCode(dto.getWarehouseCode());
        entity.setAddress(dto.getAddress());
        entity.setRegion(dto.getRegion());
        entity.setManagerId(dto.getManagerId());
        entity.setSafetyStock(dto.getSafetyStock());
    }
}
