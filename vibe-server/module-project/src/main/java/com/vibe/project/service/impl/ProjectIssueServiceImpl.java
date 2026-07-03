package com.vibe.project.service.impl;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.ProjectIssueDTO;
import com.vibe.project.entity.ProjectIssueEntity;
import com.vibe.project.mapper.ProjectIssueMapper;
import com.vibe.project.service.ProjectIssueService;
import com.vibe.project.vo.ProjectIssueVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目问题服务实现
 *
 * <p>状态流转：OPEN → PROCESSING → RESOLVED → CLOSED，
 * 进入 RESOLVED 时自动写入 resolvedTime；超期未关闭的问题由定时任务升级提醒。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectIssueServiceImpl implements ProjectIssueService {

    private final ProjectIssueMapper projectIssueMapper;

    /** 状态流转规则 */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = new HashMap<>();
    static {
        ALLOWED_TRANSITIONS.put(ProjectConstant.TRACK_OPEN,
                new HashSet<>(Arrays.asList(ProjectConstant.TRACK_PROCESSING, ProjectConstant.TRACK_CLOSED)));
        ALLOWED_TRANSITIONS.put(ProjectConstant.TRACK_PROCESSING,
                new HashSet<>(Arrays.asList(ProjectConstant.TRACK_RESOLVED, ProjectConstant.TRACK_OPEN,
                        ProjectConstant.TRACK_CLOSED)));
        ALLOWED_TRANSITIONS.put(ProjectConstant.TRACK_RESOLVED,
                new HashSet<>(Arrays.asList(ProjectConstant.TRACK_CLOSED, ProjectConstant.TRACK_PROCESSING)));
        ALLOWED_TRANSITIONS.put(ProjectConstant.TRACK_CLOSED, new HashSet<>());
    }

    @Override
    public List<ProjectIssueVO> listByProjectId(Long projectId) {
        List<ProjectIssueEntity> list = projectIssueMapper.selectByProjectId(projectId);
        return list.stream().map(ProjectConverters::toIssueVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectIssueDTO dto) {
        ProjectIssueEntity entity = new ProjectIssueEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setTaskId(dto.getTaskId());
        entity.setIssueDesc(dto.getIssueDesc());
        entity.setImpact(dto.getImpact());
        entity.setOwnerId(dto.getOwnerId());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : ProjectConstant.TRACK_OPEN);
        entity.setDueDate(dto.getDueDate());
        projectIssueMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProjectIssueDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "问题ID不能为空");
        }
        ProjectIssueEntity exist = projectIssueMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "问题不存在");
        }
        if (StringUtils.hasText(dto.getIssueDesc())) {
            exist.setIssueDesc(dto.getIssueDesc());
        }
        if (dto.getImpact() != null) {
            exist.setImpact(dto.getImpact());
        }
        if (dto.getTaskId() != null) {
            exist.setTaskId(dto.getTaskId());
        }
        if (dto.getOwnerId() != null) {
            exist.setOwnerId(dto.getOwnerId());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            exist.setStatus(dto.getStatus());
        }
        if (dto.getDueDate() != null) {
            exist.setDueDate(dto.getDueDate());
        }
        projectIssueMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectIssueEntity exist = projectIssueMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "问题不存在");
        }
        projectIssueMapper.deleteById(id);
    }

    @Override
    public ProjectIssueVO getDetail(Long id) {
        ProjectIssueEntity entity = projectIssueMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "问题不存在");
        }
        return ProjectConverters.toIssueVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long id, String targetStatus) {
        ProjectIssueEntity exist = projectIssueMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "问题不存在");
        }
        String current = exist.getStatus();
        Set<String> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    String.format("问题状态流转非法：%s → %s", current, targetStatus));
        }
        exist.setStatus(targetStatus);
        // 进入 RESOLVED 时记录解决时间
        if (ProjectConstant.TRACK_RESOLVED.equals(targetStatus)) {
            exist.setResolvedTime(LocalDateTime.now());
        }
        // 从 RESOLVED 退回 PROCESSING 时清空解决时间
        if (ProjectConstant.TRACK_PROCESSING.equals(targetStatus)
                && ProjectConstant.TRACK_RESOLVED.equals(current)) {
            exist.setResolvedTime(null);
        }
        projectIssueMapper.updateById(exist);
    }

    @Override
    public List<ProjectIssueVO> listOverdue() {
        List<ProjectIssueEntity> list = projectIssueMapper.selectOverdueIssues(LocalDate.now());
        return list.stream().map(ProjectConverters::toIssueVo).collect(Collectors.toList());
    }
}
