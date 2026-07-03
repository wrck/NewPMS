package com.vibe.project.service.impl;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.ProjectMilestoneDTO;
import com.vibe.project.entity.ProjectMilestoneEntity;
import com.vibe.project.mapper.ProjectMilestoneMapper;
import com.vibe.project.service.ProjectMilestoneService;
import com.vibe.project.vo.ProjectMilestoneVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目里程碑服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {

    private final ProjectMilestoneMapper projectMilestoneMapper;

    @Override
    public List<ProjectMilestoneVO> listByProjectId(Long projectId) {
        List<ProjectMilestoneEntity> list = projectMilestoneMapper.selectByProjectId(projectId);
        return list.stream().map(ProjectConverters::toMilestoneVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectMilestoneDTO dto) {
        ProjectMilestoneEntity entity = new ProjectMilestoneEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setMilestoneName(dto.getMilestoneName());
        entity.setPlannedDate(dto.getPlannedDate());
        entity.setActualDate(dto.getActualDate());
        entity.setDeliverables(dto.getDeliverables());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : ProjectConstant.MILESTONE_PENDING);
        projectMilestoneMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProjectMilestoneDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "里程碑ID不能为空");
        }
        ProjectMilestoneEntity exist = projectMilestoneMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "里程碑不存在");
        }
        if (StringUtils.hasText(dto.getMilestoneName())) {
            exist.setMilestoneName(dto.getMilestoneName());
        }
        if (dto.getPlannedDate() != null) {
            exist.setPlannedDate(dto.getPlannedDate());
        }
        if (dto.getActualDate() != null) {
            exist.setActualDate(dto.getActualDate());
        }
        if (dto.getDeliverables() != null) {
            exist.setDeliverables(dto.getDeliverables());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            exist.setStatus(dto.getStatus());
        }
        projectMilestoneMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectMilestoneEntity exist = projectMilestoneMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "里程碑不存在");
        }
        projectMilestoneMapper.deleteById(id);
    }

    @Override
    public ProjectMilestoneVO getDetail(Long id) {
        ProjectMilestoneEntity entity = projectMilestoneMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "里程碑不存在");
        }
        return ProjectConverters.toMilestoneVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReached(Long id, LocalDate actualDate) {
        ProjectMilestoneEntity exist = projectMilestoneMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "里程碑不存在");
        }
        exist.setActualDate(actualDate == null ? LocalDate.now() : actualDate);
        exist.setStatus(ProjectConstant.MILESTONE_REACHED);
        projectMilestoneMapper.updateById(exist);
    }
}
