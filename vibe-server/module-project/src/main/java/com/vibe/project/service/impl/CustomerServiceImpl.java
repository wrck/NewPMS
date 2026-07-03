package com.vibe.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.CustomerDTO;
import com.vibe.project.dto.CustomerQueryDTO;
import com.vibe.project.entity.CustomerEntity;
import com.vibe.project.mapper.CustomerMapper;
import com.vibe.project.service.CustomerService;
import com.vibe.project.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public PageResult<CustomerVO> page(CustomerQueryDTO query) {
        LambdaQueryWrapper<CustomerEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(CustomerEntity::getCustomerName, query.getKeyword())
                    .or().like(CustomerEntity::getCustomerCode, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getRegion())) {
            wrapper.eq(CustomerEntity::getRegion, query.getRegion());
        }
        if (StringUtils.hasText(query.getIndustry())) {
            wrapper.eq(CustomerEntity::getIndustry, query.getIndustry());
        }
        wrapper.orderByDesc(CustomerEntity::getCreateTime);

        IPage<CustomerEntity> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<CustomerEntity> result = customerMapper.selectPage(page, wrapper);
        List<CustomerVO> records = result.getRecords().stream()
                .map(ProjectConverters::toCustomerVo)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerDTO dto) {
        checkCodeUnique(dto.getCustomerCode(), null);
        CustomerEntity entity = new CustomerEntity();
        entity.setCustomerName(dto.getCustomerName());
        entity.setCustomerCode(dto.getCustomerCode());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setContactEmail(dto.getContactEmail());
        entity.setAddress(dto.getAddress());
        entity.setRegion(dto.getRegion());
        entity.setIndustry(dto.getIndustry());
        entity.setRemark(dto.getRemark());
        customerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CustomerDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "客户ID不能为空");
        }
        CustomerEntity exist = customerMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getCustomerCode())
                && !dto.getCustomerCode().equals(exist.getCustomerCode())) {
            checkCodeUnique(dto.getCustomerCode(), dto.getId());
            exist.setCustomerCode(dto.getCustomerCode());
        }
        exist.setCustomerName(dto.getCustomerName());
        exist.setContactName(dto.getContactName());
        exist.setContactPhone(dto.getContactPhone());
        exist.setContactEmail(dto.getContactEmail());
        exist.setAddress(dto.getAddress());
        exist.setRegion(dto.getRegion());
        exist.setIndustry(dto.getIndustry());
        exist.setRemark(dto.getRemark());
        customerMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CustomerEntity exist = customerMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        customerMapper.deleteById(id);
    }

    @Override
    public CustomerVO getDetail(Long id) {
        CustomerEntity entity = customerMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND);
        }
        return ProjectConverters.toCustomerVo(entity);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<CustomerEntity> wrapper = new LambdaQueryWrapper<CustomerEntity>()
                .eq(CustomerEntity::getCustomerCode, code);
        if (excludeId != null) {
            wrapper.ne(CustomerEntity::getId, excludeId);
        }
        if (customerMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "客户编码已存在");
        }
    }
}
