package com.vibe.acceptance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.acceptance.constant.AcceptanceConstant;
import com.vibe.acceptance.dto.AcceptanceIssueQueryDTO;
import com.vibe.acceptance.dto.AcceptanceIssueSaveDTO;
import com.vibe.acceptance.entity.AcceptanceIssueEntity;
import com.vibe.acceptance.mapper.AcceptanceIssueMapper;
import com.vibe.acceptance.service.AcceptanceIssueService;
import com.vibe.acceptance.vo.AcceptanceIssueVO;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 验收遗留问题 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class AcceptanceIssueServiceImpl implements AcceptanceIssueService {

    private final AcceptanceIssueMapper issueMapper;

    @Override
    public PageResult<AcceptanceIssueVO> page(AcceptanceIssueQueryDTO query) {
        LambdaQueryWrapper<AcceptanceIssueEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getProjectId() != null) {
            wrapper.eq(AcceptanceIssueEntity::getProjectId, query.getProjectId());
        }
        if (query.getTaskId() != null) {
            wrapper.eq(AcceptanceIssueEntity::getTaskId, query.getTaskId());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(AcceptanceIssueEntity::getStatus, query.getStatus());
        }
        if (query.getSeverity() != null && !query.getSeverity().isBlank()) {
            wrapper.eq(AcceptanceIssueEntity::getSeverity, query.getSeverity());
        }
        if (query.getAssigneeId() != null) {
            wrapper.eq(AcceptanceIssueEntity::getAssigneeId, query.getAssigneeId());
        }
        wrapper.orderByDesc(AcceptanceIssueEntity::getCreateTime);

        Page<AcceptanceIssueEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<AcceptanceIssueEntity> result = issueMapper.selectPage(page, wrapper);

        List<AcceptanceIssueVO> records = new ArrayList<>();
        for (AcceptanceIssueEntity e : result.getRecords()) {
            AcceptanceIssueVO vo = new AcceptanceIssueVO();
            BeanUtils.copyProperties(e, vo);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public AcceptanceIssueVO getDetail(Long id) {
        AcceptanceIssueEntity entity = issueMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("遗留问题");
        }
        AcceptanceIssueVO vo = new AcceptanceIssueVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(AcceptanceIssueSaveDTO dto) {
        AcceptanceIssueEntity entity = new AcceptanceIssueEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(AcceptanceConstant.ISSUE_STATUS_OPEN);
        }
        if (entity.getSeverity() == null) {
            entity.setSeverity("MEDIUM");
        }
        issueMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AcceptanceIssueSaveDTO dto) {
        AcceptanceIssueEntity entity = issueMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("遗留问题");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        issueMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AcceptanceIssueEntity entity = issueMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("遗留问题");
        }
        issueMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, Long assigneeId) {
        AcceptanceIssueEntity entity = issueMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("遗留问题");
        }
        entity.setAssigneeId(assigneeId);
        if (AcceptanceConstant.ISSUE_STATUS_OPEN.equals(entity.getStatus())) {
            entity.setStatus(AcceptanceConstant.ISSUE_STATUS_IN_PROGRESS);
        }
        issueMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long id) {
        AcceptanceIssueEntity entity = issueMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("遗留问题");
        }
        entity.setStatus(AcceptanceConstant.ISSUE_STATUS_RESOLVED);
        entity.setResolvedTime(LocalDateTime.now());
        issueMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        AcceptanceIssueEntity entity = issueMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("遗留问题");
        }
        if (!AcceptanceConstant.ISSUE_STATUS_RESOLVED.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅已整改状态的问题可闭环确认");
        }
        entity.setStatus(AcceptanceConstant.ISSUE_STATUS_CLOSED);
        entity.setCloseUserId(UserContextHolder.getUserId());
        entity.setCloseTime(LocalDateTime.now());
        issueMapper.updateById(entity);
    }
}
