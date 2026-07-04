package com.vibe.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.integration.dto.IntegrationCallLogQueryDTO;
import com.vibe.integration.entity.IntegrationCallLogEntity;
import com.vibe.integration.mapper.IntegrationCallLogMapper;
import com.vibe.integration.service.IntegrationCallLogService;
import com.vibe.integration.vo.IntegrationCallLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 集成调用日志 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class IntegrationCallLogServiceImpl implements IntegrationCallLogService {

    private final IntegrationCallLogMapper callLogMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<IntegrationCallLogVO> page(IntegrationCallLogQueryDTO query) {
        LambdaQueryWrapper<IntegrationCallLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getConfigId() != null) {
            wrapper.eq(IntegrationCallLogEntity::getConfigId, query.getConfigId());
        }
        if (query.getSystemCode() != null && !query.getSystemCode().isBlank()) {
            wrapper.eq(IntegrationCallLogEntity::getSystemCode, query.getSystemCode());
        }
        if (query.getCallScene() != null && !query.getCallScene().isBlank()) {
            wrapper.eq(IntegrationCallLogEntity::getCallScene, query.getCallScene());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(IntegrationCallLogEntity::getStatus, query.getStatus());
        }
        if (query.getStartBegin() != null && !query.getStartBegin().isBlank()) {
            wrapper.ge(IntegrationCallLogEntity::getOperatedAt, LocalDateTime.parse(query.getStartBegin(), DATE_FMT));
        }
        if (query.getStartEnd() != null && !query.getStartEnd().isBlank()) {
            wrapper.le(IntegrationCallLogEntity::getOperatedAt, LocalDateTime.parse(query.getStartEnd(), DATE_FMT));
        }
        wrapper.orderByDesc(IntegrationCallLogEntity::getOperatedAt);

        Page<IntegrationCallLogEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<IntegrationCallLogEntity> result = callLogMapper.selectPage(page, wrapper);

        List<IntegrationCallLogVO> records = new ArrayList<>();
        for (IntegrationCallLogEntity e : result.getRecords()) {
            records.add(toVO(e));
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public IntegrationCallLogVO getDetail(Long id) {
        IntegrationCallLogEntity entity = callLogMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("调用日志");
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        IntegrationCallLogEntity entity = callLogMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("调用日志");
        }
        callLogMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll() {
        callLogMapper.delete(new LambdaQueryWrapper<>());
    }

    /* ============ 私有方法 ============ */

    private IntegrationCallLogVO toVO(IntegrationCallLogEntity e) {
        IntegrationCallLogVO vo = new IntegrationCallLogVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
