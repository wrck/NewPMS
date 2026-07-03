package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.converter.SysConverters;
import com.vibe.system.dto.SysDictTypeDTO;
import com.vibe.system.dto.SysDictTypeQueryDTO;
import com.vibe.system.entity.SysDictTypeEntity;
import com.vibe.system.mapper.SysDictTypeMapper;
import com.vibe.system.service.SysDictDataService;
import com.vibe.system.service.SysDictTypeService;
import com.vibe.system.vo.SysDictTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 字典类型服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl implements SysDictTypeService {

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictDataService sysDictDataService;

    @Override
    public PageResult<SysDictTypeVO> page(SysDictTypeQueryDTO query) {
        IPage<SysDictTypeVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysDictTypeVO> result = sysDictTypeMapper.selectDictTypePage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysDictTypeDTO dto) {
        checkDictTypeUnique(dto.getDictType(), null);
        SysDictTypeEntity entity = new SysDictTypeEntity();
        entity.setDictName(dto.getDictName());
        entity.setDictType(dto.getDictType());
        entity.setStatus(dto.getStatus() == null ? SystemConstant.STATUS_ENABLED : dto.getStatus());
        entity.setRemark(dto.getRemark());
        sysDictTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysDictTypeDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "字典ID不能为空");
        }
        SysDictTypeEntity exist = sysDictTypeMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典类型不存在");
        }
        String oldType = exist.getDictType();
        if (dto.getDictType() != null && !dto.getDictType().equals(oldType)) {
            checkDictTypeUnique(dto.getDictType(), dto.getId());
        }
        exist.setDictName(dto.getDictName());
        exist.setDictType(dto.getDictType());
        if (dto.getStatus() != null) {
            exist.setStatus(dto.getStatus());
        }
        exist.setRemark(dto.getRemark());
        sysDictTypeMapper.updateById(exist);
        // dictType 变更或更新后，清除旧缓存
        if (oldType != null) {
            sysDictDataService.clearCache(oldType);
        }
        if (dto.getDictType() != null && !dto.getDictType().equals(oldType)) {
            sysDictDataService.clearCache(dto.getDictType());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDictTypeEntity exist = sysDictTypeMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典类型不存在");
        }
        sysDictTypeMapper.deleteById(id);
        // 清除对应字典数据缓存
        sysDictDataService.clearCache(exist.getDictType());
    }

    @Override
    public SysDictTypeVO getDetail(Long id) {
        SysDictTypeEntity entity = sysDictTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典类型不存在");
        }
        return SysConverters.toDictTypeVo(entity);
    }

    private void checkDictTypeUnique(String dictType, Long excludeId) {
        LambdaQueryWrapper<SysDictTypeEntity> wrapper = new LambdaQueryWrapper<SysDictTypeEntity>()
                .eq(SysDictTypeEntity::getDictType, dictType);
        if (excludeId != null) {
            wrapper.ne(SysDictTypeEntity::getId, excludeId);
        }
        if (sysDictTypeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "字典类型编码已存在");
        }
    }
}
