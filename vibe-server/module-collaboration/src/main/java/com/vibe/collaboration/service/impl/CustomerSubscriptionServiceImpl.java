package com.vibe.collaboration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.collaboration.dto.CustomerSubscriptionDTO;
import com.vibe.collaboration.entity.CustomerSubscriptionEntity;
import com.vibe.collaboration.mapper.CustomerSubscriptionMapper;
import com.vibe.collaboration.service.CustomerSubscriptionService;
import com.vibe.collaboration.vo.CustomerSubscriptionVO;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 客户订阅服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerSubscriptionServiceImpl implements CustomerSubscriptionService {

    /** 默认订阅状态 */
    private static final String DEFAULT_STATUS = "SUBSCRIBED";

    private final CustomerSubscriptionMapper customerSubscriptionMapper;

    @Override
    public CustomerSubscriptionVO getDetail(Long id) {
        CustomerSubscriptionEntity entity = customerSubscriptionMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerSubscriptionDTO dto) {
        if (dto.getCustomerId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "客户ID不能为空");
        }
        checkEventTypeUnique(dto.getCustomerId(), dto.getEventType(), null);
        CustomerSubscriptionEntity entity = new CustomerSubscriptionEntity();
        copyDtoToEntity(dto, entity);
        customerSubscriptionMapper.insert(entity);
        log.info("[CustomerSubscription] 新增订阅: customerId={}, eventType={}",
                entity.getCustomerId(), entity.getEventType());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CustomerSubscriptionDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "订阅ID不能为空");
        }
        CustomerSubscriptionEntity exist = customerSubscriptionMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        Long customerId = dto.getCustomerId() != null ? dto.getCustomerId() : exist.getCustomerId();
        if (dto.getEventType() != null && !dto.getEventType().equals(exist.getEventType())) {
            checkEventTypeUnique(customerId, dto.getEventType(), dto.getId());
        }
        copyDtoToEntity(dto, exist);
        customerSubscriptionMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CustomerSubscriptionEntity exist = customerSubscriptionMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        customerSubscriptionMapper.deleteById(id);
    }

    @Override
    public List<CustomerSubscriptionVO> listByCustomerId(Long customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        List<CustomerSubscriptionEntity> list = customerSubscriptionMapper.selectList(
                new LambdaQueryWrapper<CustomerSubscriptionEntity>()
                        .eq(CustomerSubscriptionEntity::getCustomerId, customerId)
                        .orderByDesc(CustomerSubscriptionEntity::getCreateTime));
        return list == null ? Collections.emptyList() : list.stream().map(this::toVO).toList();
    }

    @Override
    public CustomerSubscriptionVO getByCustomerIdAndEventType(Long customerId, String eventType) {
        if (customerId == null || !StringUtils.hasText(eventType)) {
            return null;
        }
        CustomerSubscriptionEntity entity = customerSubscriptionMapper.selectOne(
                new LambdaQueryWrapper<CustomerSubscriptionEntity>()
                        .eq(CustomerSubscriptionEntity::getCustomerId, customerId)
                        .eq(CustomerSubscriptionEntity::getEventType, eventType)
                        .last("LIMIT 1"));
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubscriptions(Long customerId, List<CustomerSubscriptionDTO> dtos) {
        if (customerId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "客户ID不能为空");
        }
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        for (CustomerSubscriptionDTO dto : dtos) {
            CustomerSubscriptionEntity exist = customerSubscriptionMapper.selectOne(
                    new LambdaQueryWrapper<CustomerSubscriptionEntity>()
                            .eq(CustomerSubscriptionEntity::getCustomerId, customerId)
                            .eq(CustomerSubscriptionEntity::getEventType, dto.getEventType())
                            .last("LIMIT 1"));
            if (exist == null) {
                CustomerSubscriptionEntity entity = new CustomerSubscriptionEntity();
                entity.setCustomerId(customerId);
                entity.setEventType(dto.getEventType());
                entity.setChannels(dto.getChannels());
                entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : DEFAULT_STATUS);
                customerSubscriptionMapper.insert(entity);
            } else {
                exist.setChannels(dto.getChannels());
                if (StringUtils.hasText(dto.getStatus())) {
                    exist.setStatus(dto.getStatus());
                }
                customerSubscriptionMapper.updateById(exist);
            }
        }
        log.info("[CustomerSubscription] 批量更新订阅: customerId={}, count={}", customerId, dtos.size());
    }

    /* ============ 私有辅助方法 ============ */

    private void checkEventTypeUnique(Long customerId, String eventType, Long excludeId) {
        LambdaQueryWrapper<CustomerSubscriptionEntity> wrapper = new LambdaQueryWrapper<CustomerSubscriptionEntity>()
                .eq(CustomerSubscriptionEntity::getCustomerId, customerId)
                .eq(CustomerSubscriptionEntity::getEventType, eventType);
        if (excludeId != null) {
            wrapper.ne(CustomerSubscriptionEntity::getId, excludeId);
        }
        if (customerSubscriptionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "同类型订阅已存在");
        }
    }

    private void copyDtoToEntity(CustomerSubscriptionDTO dto, CustomerSubscriptionEntity entity) {
        if (dto.getCustomerId() != null) {
            entity.setCustomerId(dto.getCustomerId());
        }
        entity.setEventType(dto.getEventType());
        entity.setChannels(dto.getChannels());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : DEFAULT_STATUS);
    }

    private CustomerSubscriptionVO toVO(CustomerSubscriptionEntity entity) {
        if (entity == null) {
            return null;
        }
        CustomerSubscriptionVO vo = new CustomerSubscriptionVO();
        vo.setId(entity.getId());
        vo.setCustomerId(entity.getCustomerId());
        vo.setEventType(entity.getEventType());
        vo.setChannels(entity.getChannels());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
