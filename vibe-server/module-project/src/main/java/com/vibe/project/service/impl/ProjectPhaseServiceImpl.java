package com.vibe.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.ProjectPhaseDTO;
import com.vibe.project.entity.ProjectPhaseEntity;
import com.vibe.project.mapper.ProjectPhaseMapper;
import com.vibe.project.service.ProjectPhaseService;
import com.vibe.project.vo.ProjectPhaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目阶段服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectPhaseServiceImpl implements ProjectPhaseService {

    private final ProjectPhaseMapper projectPhaseMapper;

    @Override
    public List<ProjectPhaseVO> listByProjectId(Long projectId) {
        List<ProjectPhaseEntity> phases = projectPhaseMapper.selectByProjectId(projectId);
        return phases.stream().map(ProjectConverters::toPhaseVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectPhaseDTO dto) {
        validateDateRange(dto.getPlannedStart(), dto.getPlannedEnd());
        ProjectPhaseEntity entity = new ProjectPhaseEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setPhaseCode(dto.getPhaseCode());
        entity.setPhaseName(dto.getPhaseName());
        entity.setSortOrder(dto.getSortOrder());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : ProjectConstant.PHASE_NOT_STARTED);
        entity.setPlannedStart(dto.getPlannedStart());
        entity.setPlannedEnd(dto.getPlannedEnd());
        entity.setDeliverables(dto.getDeliverables());
        projectPhaseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProjectPhaseDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "阶段ID不能为空");
        }
        ProjectPhaseEntity exist = projectPhaseMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "阶段不存在");
        }
        validateDateRange(dto.getPlannedStart(), dto.getPlannedEnd());
        if (StringUtils.hasText(dto.getPhaseCode())) {
            exist.setPhaseCode(dto.getPhaseCode());
        }
        if (StringUtils.hasText(dto.getPhaseName())) {
            exist.setPhaseName(dto.getPhaseName());
        }
        if (dto.getSortOrder() != null) {
            exist.setSortOrder(dto.getSortOrder());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            exist.setStatus(dto.getStatus());
        }
        if (dto.getPlannedStart() != null) {
            exist.setPlannedStart(dto.getPlannedStart());
        }
        if (dto.getPlannedEnd() != null) {
            exist.setPlannedEnd(dto.getPlannedEnd());
        }
        if (dto.getActualStart() != null) {
            exist.setActualStart(dto.getActualStart());
        }
        if (dto.getActualEnd() != null) {
            exist.setActualEnd(dto.getActualEnd());
        }
        if (dto.getDeliverables() != null) {
            exist.setDeliverables(dto.getDeliverables());
        }
        projectPhaseMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectPhaseEntity exist = projectPhaseMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "阶段不存在");
        }
        projectPhaseMapper.deleteById(id);
    }

    @Override
    public ProjectPhaseVO getDetail(Long id) {
        ProjectPhaseEntity entity = projectPhaseMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "阶段不存在");
        }
        return ProjectConverters.toPhaseVo(entity);
    }

    /**
     * 校验时间范围：计划开始不得晚于计划结束
     */
    private void validateDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "计划开始时间不能晚于计划结束时间");
        }
    }
}
