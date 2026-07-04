package com.vibe.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.integration.constant.IntegrationConstant;
import com.vibe.integration.dto.IntegrationConfigQueryDTO;
import com.vibe.integration.dto.IntegrationConfigSaveDTO;
import com.vibe.integration.entity.IntegrationConfigEntity;
import com.vibe.integration.mapper.IntegrationConfigMapper;
import com.vibe.integration.service.IntegrationConfigService;
import com.vibe.integration.vo.IntegrationConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 集成配置 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class IntegrationConfigServiceImpl implements IntegrationConfigService {

    private final IntegrationConfigMapper configMapper;

    @Override
    public PageResult<IntegrationConfigVO> page(IntegrationConfigQueryDTO query) {
        LambdaQueryWrapper<IntegrationConfigEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(IntegrationConfigEntity::getSystemCode, query.getKeyword())
                    .or().like(IntegrationConfigEntity::getSystemName, query.getKeyword()));
        }
        if (query.getAdapterType() != null && !query.getAdapterType().isBlank()) {
            wrapper.eq(IntegrationConfigEntity::getAdapterType, query.getAdapterType());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(IntegrationConfigEntity::getEnabled, query.getEnabled());
        }
        wrapper.orderByDesc(IntegrationConfigEntity::getCreateTime);

        Page<IntegrationConfigEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<IntegrationConfigEntity> result = configMapper.selectPage(page, wrapper);

        List<IntegrationConfigVO> records = new ArrayList<>();
        for (IntegrationConfigEntity e : result.getRecords()) {
            records.add(toVO(e));
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public IntegrationConfigVO getDetail(Long id) {
        IntegrationConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("集成配置");
        }
        return toVO(entity);
    }

    @Override
    public IntegrationConfigVO getBySystemCode(String systemCode) {
        LambdaQueryWrapper<IntegrationConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IntegrationConfigEntity::getSystemCode, systemCode);
        IntegrationConfigEntity entity = configMapper.selectOne(wrapper);
        return entity == null ? null : toVO(entity);
    }

    @Override
    public List<IntegrationConfigVO> listEnabled() {
        LambdaQueryWrapper<IntegrationConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IntegrationConfigEntity::getEnabled, 1)
                .orderByAsc(IntegrationConfigEntity::getSystemCode);
        List<IntegrationConfigEntity> list = configMapper.selectList(wrapper);
        List<IntegrationConfigVO> result = new ArrayList<>(list.size());
        for (IntegrationConfigEntity e : list) {
            result.add(toVO(e));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(IntegrationConfigSaveDTO dto) {
        // 校验 systemCode 唯一
        LambdaQueryWrapper<IntegrationConfigEntity> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(IntegrationConfigEntity::getSystemCode, dto.getSystemCode());
        Long existCount = configMapper.selectCount(existWrapper);
        if (existCount != null && existCount > 0) {
            throw BusinessException.of(400, "系统编码已存在: " + dto.getSystemCode());
        }
        IntegrationConfigEntity entity = new IntegrationConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getAdapterType() == null || entity.getAdapterType().isBlank()) {
            entity.setAdapterType(IntegrationConstant.ADAPTER_REST_API);
        }
        if (entity.getAuthType() == null || entity.getAuthType().isBlank()) {
            entity.setAuthType(IntegrationConstant.AUTH_NONE);
        }
        if (entity.getTimeoutMs() == null) {
            entity.setTimeoutMs(10000);
        }
        if (entity.getRetryCount() == null) {
            entity.setRetryCount(0);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        configMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, IntegrationConfigSaveDTO dto) {
        IntegrationConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("集成配置");
        }
        // 如果 systemCode 修改了，校验唯一
        if (!entity.getSystemCode().equals(dto.getSystemCode())) {
            LambdaQueryWrapper<IntegrationConfigEntity> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(IntegrationConfigEntity::getSystemCode, dto.getSystemCode());
            Long existCount = configMapper.selectCount(existWrapper);
            if (existCount != null && existCount > 0) {
                throw BusinessException.of(400, "系统编码已存在: " + dto.getSystemCode());
            }
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        configMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        IntegrationConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("集成配置");
        }
        configMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long id, Integer enabled) {
        IntegrationConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("集成配置");
        }
        entity.setEnabled(enabled);
        configMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean testConnection(Long id) {
        IntegrationConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("集成配置");
        }
        // MVP 阶段：仅检查配置可用性并更新最近调用状态
        // 实际的 HTTP 调用由 Phase 2 的适配器层完成
        boolean reachable = entity.getEndpointUrl() != null && !entity.getEndpointUrl().isBlank();
        entity.setLastCallTime(LocalDateTime.now());
        entity.setLastCallStatus(reachable
                ? IntegrationConstant.CALL_STATUS_SUCCESS
                : IntegrationConstant.CALL_STATUS_FAIL);
        configMapper.updateById(entity);
        return reachable;
    }

    /* ============ 私有方法 ============ */

    private IntegrationConfigVO toVO(IntegrationConfigEntity e) {
        IntegrationConfigVO vo = new IntegrationConfigVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
