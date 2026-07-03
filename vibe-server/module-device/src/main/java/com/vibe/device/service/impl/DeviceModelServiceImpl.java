package com.vibe.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.device.dto.DeviceModelDTO;
import com.vibe.device.dto.DeviceModelQueryDTO;
import com.vibe.device.entity.DeviceModelEntity;
import com.vibe.device.mapper.DeviceModelMapper;
import com.vibe.device.service.DeviceModelService;
import com.vibe.device.vo.DeviceModelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 设备型号服务实现。
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceModelServiceImpl implements DeviceModelService {

    private final DeviceModelMapper deviceModelMapper;

    @Override
    public PageResult<DeviceModelVO> page(DeviceModelQueryDTO query) {
        List<DeviceModelVO> all = deviceModelMapper.selectModelList(
                query.getKeyword(), query.getProductLine(), query.getCategory());
        int total = all.size();
        int p = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int s = query.getSize() == null || query.getSize() < 1 ? 20 : query.getSize();
        int from = Math.min((p - 1) * s, total);
        int to = Math.min(from + s, total);
        return PageResult.of(all.subList(from, to), total, p, s);
    }

    @Override
    public List<DeviceModelVO> list(String productLine, String category) {
        return deviceModelMapper.selectModelList(null, productLine, category);
    }

    @Override
    public List<DeviceModelVO> listAll() {
        return deviceModelMapper.selectModelList(null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DeviceModelDTO dto) {
        checkCodeUnique(dto.getModelCode(), null);
        DeviceModelEntity entity = new DeviceModelEntity();
        copyDtoToEntity(dto, entity);
        deviceModelMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeviceModelDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "型号ID不能为空");
        }
        DeviceModelEntity exist = deviceModelMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备型号不存在");
        }
        if (dto.getModelCode() != null && !dto.getModelCode().equals(exist.getModelCode())) {
            checkCodeUnique(dto.getModelCode(), dto.getId());
        }
        copyDtoToEntity(dto, exist);
        deviceModelMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (deviceModelMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备型号不存在");
        }
        deviceModelMapper.deleteById(id);
    }

    @Override
    public DeviceModelVO getDetail(Long id) {
        DeviceModelVO vo = deviceModelMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "设备型号不存在");
        }
        return vo;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<DeviceModelEntity> wrapper = new LambdaQueryWrapper<DeviceModelEntity>()
                .eq(DeviceModelEntity::getModelCode, code);
        if (excludeId != null) {
            wrapper.ne(DeviceModelEntity::getId, excludeId);
        }
        if (deviceModelMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "型号编码已存在");
        }
    }

    private void copyDtoToEntity(DeviceModelDTO dto, DeviceModelEntity entity) {
        entity.setModelCode(dto.getModelCode());
        entity.setModelName(dto.getModelName());
        entity.setProductLine(dto.getProductLine());
        entity.setVendor(dto.getVendor());
        entity.setCategory(dto.getCategory());
        entity.setSpecifications(dto.getSpecifications());
        entity.setConfigTemplate(dto.getConfigTemplate());
        entity.setManualUrl(dto.getManualUrl());
        entity.setImageUrl(dto.getImageUrl());
    }
}
