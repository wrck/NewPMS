package com.vibe.acceptance.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
        IPage<AcceptanceIssueVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<AcceptanceIssueVO> result = issueMapper.selectIssuePage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AcceptanceIssueVO getDetail(Long id) {
        AcceptanceIssueVO vo = issueMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.notFound("遗留问题");
        }
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
