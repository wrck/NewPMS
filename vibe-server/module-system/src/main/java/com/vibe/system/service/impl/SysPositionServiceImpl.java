package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysPositionDTO;
import com.vibe.system.entity.SysPositionEntity;
import com.vibe.system.mapper.SysPositionMapper;
import com.vibe.system.service.SysPositionService;
import com.vibe.system.vo.SysPositionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 岗位服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPositionServiceImpl implements SysPositionService {

    private final SysPositionMapper sysPositionMapper;

    @Override
    public PageResult<SysPositionVO> page(Integer page, Integer size, String keyword, Long orgId) {
        IPage<SysPositionVO> p = new Page<>(
                page == null ? 1 : page,
                size == null ? 20 : size);
        IPage<SysPositionVO> result = sysPositionMapper.selectPositionPage(p, keyword, orgId);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysPositionDTO dto) {
        checkCodeUnique(dto.getPositionCode(), null);
        SysPositionEntity entity = new SysPositionEntity();
        copyDtoToEntity(dto, entity);
        sysPositionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysPositionDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "岗位ID不能为空");
        }
        SysPositionEntity exist = sysPositionMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        if (dto.getPositionCode() != null && !dto.getPositionCode().equals(exist.getPositionCode())) {
            checkCodeUnique(dto.getPositionCode(), dto.getId());
        }
        copyDtoToEntity(dto, exist);
        sysPositionMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (sysPositionMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        sysPositionMapper.deleteById(id);
    }

    @Override
    public SysPositionVO getDetail(Long id) {
        SysPositionVO vo = sysPositionMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        return vo;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysPositionEntity> wrapper = new LambdaQueryWrapper<SysPositionEntity>()
                .eq(SysPositionEntity::getPositionCode, code);
        if (excludeId != null) {
            wrapper.ne(SysPositionEntity::getId, excludeId);
        }
        if (sysPositionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "岗位编码已存在");
        }
    }

    private void copyDtoToEntity(SysPositionDTO dto, SysPositionEntity entity) {
        entity.setOrgId(dto.getOrgId());
        entity.setPositionName(dto.getPositionName());
        entity.setPositionCode(dto.getPositionCode());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? SystemConstant.STATUS_ENABLED : dto.getStatus());
    }
}
