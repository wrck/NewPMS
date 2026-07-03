package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.constant.RedisKeyConstant;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.converter.SysConverters;
import com.vibe.system.dto.SysDictDataDTO;
import com.vibe.system.dto.SysDictDataQueryDTO;
import com.vibe.system.entity.SysDictDataEntity;
import com.vibe.system.mapper.SysDictDataMapper;
import com.vibe.system.service.SysDictDataService;
import com.vibe.system.vo.SysDictDataVO;
import com.vibe.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 字典数据服务实现（带 Redis 缓存）
 *
 * <p>缓存 Key：{@code vibe:sys:dict:{dictType}}</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl implements SysDictDataService {

    private final SysDictDataMapper sysDictDataMapper;
    private final RedisUtils redisUtils;

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public PageResult<SysDictDataVO> page(SysDictDataQueryDTO query) {
        IPage<SysDictDataVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysDictDataVO> result = sysDictDataMapper.selectDictDataPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysDictDataDTO dto) {
        SysDictDataEntity entity = new SysDictDataEntity();
        copyDtoToEntity(dto, entity);
        sysDictDataMapper.insert(entity);
        clearCache(dto.getDictType());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysDictDataDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "字典数据ID不能为空");
        }
        SysDictDataEntity exist = sysDictDataMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        String oldType = exist.getDictType();
        copyDtoToEntity(dto, exist);
        sysDictDataMapper.updateById(exist);
        clearCache(oldType);
        if (dto.getDictType() != null && !dto.getDictType().equals(oldType)) {
            clearCache(dto.getDictType());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDictDataEntity exist = sysDictDataMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        sysDictDataMapper.deleteById(id);
        clearCache(exist.getDictType());
    }

    @Override
    public SysDictDataVO getDetail(Long id) {
        SysDictDataEntity entity = sysDictDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        return SysConverters.toDictDataVo(entity);
    }

    @Override
    public List<SysDictDataVO> listByDictType(String dictType) {
        String cacheKey = RedisKeyConstant.sysDict(dictType);
        List<SysDictDataVO> cached = redisUtils.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<SysDictDataVO> list = sysDictDataMapper.selectByDictType(dictType);
        redisUtils.set(cacheKey, list, CACHE_TTL);
        return list;
    }

    @Override
    public void clearCache(String dictType) {
        if (dictType == null || dictType.isEmpty()) {
            return;
        }
        redisUtils.delete(RedisKeyConstant.sysDict(dictType));
    }

    private void copyDtoToEntity(SysDictDataDTO dto, SysDictDataEntity entity) {
        entity.setDictType(dto.getDictType());
        entity.setDictLabel(dto.getDictLabel());
        entity.setDictValue(dto.getDictValue());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? SystemConstant.STATUS_ENABLED : dto.getStatus());
    }
}
