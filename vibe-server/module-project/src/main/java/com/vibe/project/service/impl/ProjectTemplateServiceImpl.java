package com.vibe.project.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.ProjectTemplateDTO;
import com.vibe.project.dto.ProjectTemplatePhaseDTO;
import com.vibe.project.dto.ProjectTemplateQueryDTO;
import com.vibe.project.dto.ProjectTemplateTaskDTO;
import com.vibe.project.entity.ProjectTemplateEntity;
import com.vibe.project.entity.ProjectTemplatePhaseEntity;
import com.vibe.project.entity.ProjectTemplateTaskEntity;
import com.vibe.project.mapper.ProjectTemplateMapper;
import com.vibe.project.mapper.ProjectTemplatePhaseMapper;
import com.vibe.project.mapper.ProjectTemplateTaskMapper;
import com.vibe.project.service.ProjectTemplateService;
import com.vibe.project.vo.ProjectTemplateDetailVO;
import com.vibe.project.vo.ProjectTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目模板服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private final ProjectTemplateMapper projectTemplateMapper;
    private final ProjectTemplatePhaseMapper projectTemplatePhaseMapper;
    private final ProjectTemplateTaskMapper projectTemplateTaskMapper;

    @Override
    public PageResult<ProjectTemplateVO> page(ProjectTemplateQueryDTO query) {
        IPage<ProjectTemplateVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<ProjectTemplateVO> result = projectTemplateMapper.selectTemplatePage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public ProjectTemplateDetailVO getDetail(Long id) {
        ProjectTemplateEntity entity = projectTemplateMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "项目模板不存在");
        }
        ProjectTemplateDetailVO vo = new ProjectTemplateDetailVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setProjectType(entity.getProjectType());
        vo.setProductLine(entity.getProductLine());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        List<ProjectTemplatePhaseEntity> phases = projectTemplatePhaseMapper.selectByTemplateId(id);
        vo.setPhases(phases.stream().map(ProjectConverters::toTemplatePhaseVo).collect(Collectors.toList()));
        List<ProjectTemplateTaskEntity> tasks = projectTemplateTaskMapper.selectByTemplateId(id);
        vo.setTasks(tasks.stream().map(ProjectConverters::toTemplateTaskVo).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectTemplateDTO dto) {
        ProjectTemplateEntity entity = new ProjectTemplateEntity();
        entity.setTemplateName(dto.getTemplateName());
        entity.setProjectType(dto.getProjectType());
        entity.setProductLine(dto.getProductLine());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus() == null ? ProjectConstant.TEMPLATE_ENABLED : dto.getStatus());
        projectTemplateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProjectTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "模板ID不能为空");
        }
        ProjectTemplateEntity exist = projectTemplateMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "项目模板不存在");
        }
        if (StringUtils.hasText(dto.getTemplateName())) {
            exist.setTemplateName(dto.getTemplateName());
        }
        if (dto.getProjectType() != null) {
            exist.setProjectType(dto.getProjectType());
        }
        if (dto.getProductLine() != null) {
            exist.setProductLine(dto.getProductLine());
        }
        if (dto.getDescription() != null) {
            exist.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            exist.setStatus(dto.getStatus());
        }
        projectTemplateMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectTemplateEntity exist = projectTemplateMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "项目模板不存在");
        }
        projectTemplateMapper.deleteById(id);
        // 级联逻辑删除阶段与任务
        projectTemplatePhaseMapper.deleteByTemplateId(id);
        projectTemplateTaskMapper.deleteByTemplateId(id);
    }

    /* ============ 阶段 ============ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addPhase(ProjectTemplatePhaseDTO dto) {
        ProjectTemplatePhaseEntity entity = new ProjectTemplatePhaseEntity();
        entity.setTemplateId(dto.getTemplateId());
        entity.setPhaseCode(dto.getPhaseCode());
        entity.setPhaseName(dto.getPhaseName());
        entity.setSortOrder(dto.getSortOrder());
        entity.setDeliverables(dto.getDeliverables());
        projectTemplatePhaseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePhase(ProjectTemplatePhaseDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "阶段ID不能为空");
        }
        ProjectTemplatePhaseEntity exist = projectTemplatePhaseMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "模板阶段不存在");
        }
        if (StringUtils.hasText(dto.getPhaseCode())) {
            exist.setPhaseCode(dto.getPhaseCode());
        }
        if (StringUtils.hasText(dto.getPhaseName())) {
            exist.setPhaseName(dto.getPhaseName());
        }
        if (dto.getSortOrder() != null) {
            exist.setSortOrder(dto.getSortOrder());
        }
        if (dto.getDeliverables() != null) {
            exist.setDeliverables(dto.getDeliverables());
        }
        projectTemplatePhaseMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePhase(Long id) {
        ProjectTemplatePhaseEntity exist = projectTemplatePhaseMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "模板阶段不存在");
        }
        projectTemplatePhaseMapper.deleteById(id);
    }

    /* ============ 任务 ============ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTask(ProjectTemplateTaskDTO dto) {
        ProjectTemplateTaskEntity entity = new ProjectTemplateTaskEntity();
        entity.setTemplateId(dto.getTemplateId());
        entity.setPhaseCode(dto.getPhaseCode());
        entity.setTaskName(dto.getTaskName());
        entity.setTaskType(dto.getTaskType());
        entity.setDescription(dto.getDescription());
        entity.setDefaultDays(dto.getDefaultDays());
        projectTemplateTaskMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(ProjectTemplateTaskDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "任务ID不能为空");
        }
        ProjectTemplateTaskEntity exist = projectTemplateTaskMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "模板任务不存在");
        }
        if (StringUtils.hasText(dto.getPhaseCode())) {
            exist.setPhaseCode(dto.getPhaseCode());
        }
        if (StringUtils.hasText(dto.getTaskName())) {
            exist.setTaskName(dto.getTaskName());
        }
        if (dto.getTaskType() != null) {
            exist.setTaskType(dto.getTaskType());
        }
        if (dto.getDescription() != null) {
            exist.setDescription(dto.getDescription());
        }
        if (dto.getDefaultDays() != null) {
            exist.setDefaultDays(dto.getDefaultDays());
        }
        projectTemplateTaskMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        ProjectTemplateTaskEntity exist = projectTemplateTaskMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "模板任务不存在");
        }
        projectTemplateTaskMapper.deleteById(id);
    }
}
