package com.vibe.collaboration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.collaboration.dto.CustomerPreferenceDTO;
import com.vibe.collaboration.entity.CustomerPreferenceEntity;
import com.vibe.collaboration.mapper.CustomerPreferenceMapper;
import com.vibe.collaboration.service.CustomerPreferenceService;
import com.vibe.collaboration.vo.CustomerPreferenceVO;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 客户偏好服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerPreferenceServiceImpl implements CustomerPreferenceService {

    private final CustomerPreferenceMapper customerPreferenceMapper;

    @Override
    public CustomerPreferenceVO getDetail(Long id) {
        CustomerPreferenceEntity entity = customerPreferenceMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerPreferenceDTO dto) {
        if (dto.getCustomerId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "客户ID不能为空");
        }
        checkKeyUnique(dto.getCustomerId(), dto.getPreferenceKey(), null);
        CustomerPreferenceEntity entity = new CustomerPreferenceEntity();
        copyDtoToEntity(dto, entity);
        customerPreferenceMapper.insert(entity);
        log.info("[CustomerPreference] 新增偏好: customerId={}, key={}",
                entity.getCustomerId(), entity.getPreferenceKey());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CustomerPreferenceDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "偏好ID不能为空");
        }
        CustomerPreferenceEntity exist = customerPreferenceMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        Long customerId = dto.getCustomerId() != null ? dto.getCustomerId() : exist.getCustomerId();
        if (dto.getPreferenceKey() != null && !dto.getPreferenceKey().equals(exist.getPreferenceKey())) {
            checkKeyUnique(customerId, dto.getPreferenceKey(), dto.getId());
        }
        copyDtoToEntity(dto, exist);
        customerPreferenceMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CustomerPreferenceEntity exist = customerPreferenceMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        customerPreferenceMapper.deleteById(id);
    }

    @Override
    public List<CustomerPreferenceVO> listByCustomerId(Long customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        List<CustomerPreferenceEntity> list = customerPreferenceMapper.selectList(
                new LambdaQueryWrapper<CustomerPreferenceEntity>()
                        .eq(CustomerPreferenceEntity::getCustomerId, customerId)
                        .orderByDesc(CustomerPreferenceEntity::getCreateTime));
        return list == null ? Collections.emptyList() : list.stream().map(this::toVO).toList();
    }

    @Override
    public CustomerPreferenceVO getByCustomerIdAndKey(Long customerId, String preferenceKey) {
        if (customerId == null || !StringUtils.hasText(preferenceKey)) {
            return null;
        }
        CustomerPreferenceEntity entity = customerPreferenceMapper.selectOne(
                new LambdaQueryWrapper<CustomerPreferenceEntity>()
                        .eq(CustomerPreferenceEntity::getCustomerId, customerId)
                        .eq(CustomerPreferenceEntity::getPreferenceKey, preferenceKey)
                        .last("LIMIT 1"));
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePreferences(Long customerId, List<CustomerPreferenceDTO> dtos) {
        if (customerId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "客户ID不能为空");
        }
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        for (CustomerPreferenceDTO dto : dtos) {
            // upsert：按 (customerId, preferenceKey) 定位
            CustomerPreferenceEntity exist = customerPreferenceMapper.selectOne(
                    new LambdaQueryWrapper<CustomerPreferenceEntity>()
                            .eq(CustomerPreferenceEntity::getCustomerId, customerId)
                            .eq(CustomerPreferenceEntity::getPreferenceKey, dto.getPreferenceKey())
                            .last("LIMIT 1"));
            if (exist == null) {
                CustomerPreferenceEntity entity = new CustomerPreferenceEntity();
                entity.setCustomerId(customerId);
                entity.setPreferenceKey(dto.getPreferenceKey());
                entity.setPreferenceValue(dto.getPreferenceValue());
                customerPreferenceMapper.insert(entity);
            } else {
                exist.setPreferenceValue(dto.getPreferenceValue());
                customerPreferenceMapper.updateById(exist);
            }
        }
        log.info("[CustomerPreference] 批量更新偏好: customerId={}, count={}", customerId, dtos.size());
    }

    /* ============ 私有辅助方法 ============ */

    private void checkKeyUnique(Long customerId, String key, Long excludeId) {
        LambdaQueryWrapper<CustomerPreferenceEntity> wrapper = new LambdaQueryWrapper<CustomerPreferenceEntity>()
                .eq(CustomerPreferenceEntity::getCustomerId, customerId)
                .eq(CustomerPreferenceEntity::getPreferenceKey, key);
        if (excludeId != null) {
            wrapper.ne(CustomerPreferenceEntity::getId, excludeId);
        }
        if (customerPreferenceMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "偏好键已存在");
        }
    }

    private void copyDtoToEntity(CustomerPreferenceDTO dto, CustomerPreferenceEntity entity) {
        if (dto.getCustomerId() != null) {
            entity.setCustomerId(dto.getCustomerId());
        }
        entity.setPreferenceKey(dto.getPreferenceKey());
        entity.setPreferenceValue(dto.getPreferenceValue());
    }

    private CustomerPreferenceVO toVO(CustomerPreferenceEntity entity) {
        if (entity == null) {
            return null;
        }
        CustomerPreferenceVO vo = new CustomerPreferenceVO();
        vo.setId(entity.getId());
        vo.setCustomerId(entity.getCustomerId());
        vo.setPreferenceKey(entity.getPreferenceKey());
        vo.setPreferenceValue(entity.getPreferenceValue());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
