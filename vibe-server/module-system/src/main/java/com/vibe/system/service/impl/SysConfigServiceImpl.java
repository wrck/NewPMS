package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.constant.RedisKeyConstant;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.converter.SysConverters;
import com.vibe.system.dto.SysConfigDTO;
import com.vibe.system.dto.SysConfigQueryDTO;
import com.vibe.system.entity.SysConfigEntity;
import com.vibe.system.mapper.SysConfigMapper;
import com.vibe.system.service.SysConfigService;
import com.vibe.system.vo.SysConfigVO;
import com.vibe.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 系统配置服务实现（带 Redis 缓存）
 *
 * <p>缓存 Key：{@code vibe:sys:config:{configKey}}</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final RedisUtils redisUtils;

    private static final Duration CACHE_TTL = Duration.ofMinutes(60);

    @Override
    public PageResult<SysConfigVO> page(SysConfigQueryDTO query) {
        IPage<SysConfigVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysConfigVO> result = sysConfigMapper.selectConfigPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysConfigDTO dto) {
        checkKeyUnique(dto.getConfigKey(), null);
        SysConfigEntity entity = new SysConfigEntity();
        copyDtoToEntity(dto, entity);
        sysConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysConfigDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "配置ID不能为空");
        }
        SysConfigEntity exist = sysConfigMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        String oldKey = exist.getConfigKey();
        if (dto.getConfigKey() != null && !dto.getConfigKey().equals(oldKey)) {
            checkKeyUnique(dto.getConfigKey(), dto.getId());
        }
        copyDtoToEntity(dto, exist);
        sysConfigMapper.updateById(exist);
        // 清除旧 key 缓存
        if (oldKey != null) {
            clearCache(oldKey);
        }
        if (dto.getConfigKey() != null) {
            clearCache(dto.getConfigKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysConfigEntity exist = sysConfigMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        sysConfigMapper.deleteById(id);
        clearCache(exist.getConfigKey());
    }

    @Override
    public SysConfigVO getDetail(Long id) {
        SysConfigEntity entity = sysConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        return SysConverters.toConfigVo(entity);
    }

    @Override
    public String getConfigValue(String configKey) {
        String cacheKey = RedisKeyConstant.sysConfig(configKey);
        String cached = redisUtils.getStr(cacheKey);
        if (cached != null) {
            return cached;
        }
        SysConfigEntity entity = sysConfigMapper.selectByConfigKey(configKey);
        String value = entity == null ? null : entity.getConfigValue();
        if (value != null) {
            redisUtils.set(cacheKey, value, CACHE_TTL);
        }
        return value;
    }

    @Override
    public void clearCache(String configKey) {
        if (configKey == null || configKey.isEmpty()) {
            return;
        }
        redisUtils.delete(RedisKeyConstant.sysConfig(configKey));
    }

    private void checkKeyUnique(String key, Long excludeId) {
        LambdaQueryWrapper<SysConfigEntity> wrapper = new LambdaQueryWrapper<SysConfigEntity>()
                .eq(SysConfigEntity::getConfigKey, key);
        if (excludeId != null) {
            wrapper.ne(SysConfigEntity::getId, excludeId);
        }
        if (sysConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "配置键已存在");
        }
    }

    private void copyDtoToEntity(SysConfigDTO dto, SysConfigEntity entity) {
        entity.setConfigName(dto.getConfigName());
        entity.setConfigKey(dto.getConfigKey());
        entity.setConfigValue(dto.getConfigValue());
        entity.setConfigType(StringUtils.hasText(dto.getConfigType()) ? dto.getConfigType() : "SYSTEM");
        entity.setRemark(dto.getRemark());
    }
}
