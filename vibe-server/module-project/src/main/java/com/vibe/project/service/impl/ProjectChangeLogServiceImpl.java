package com.vibe.project.service.impl;

import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.event.DomainEventPublisher;
import com.vibe.event.events.ChangeApprovedEvent;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.ChangeApproveDTO;
import com.vibe.project.dto.ProjectChangeDTO;
import com.vibe.project.entity.ProjectChangeLogEntity;
import com.vibe.project.mapper.ProjectChangeLogMapper;
import com.vibe.project.service.ProjectChangeLogService;
import com.vibe.project.vo.ProjectChangeLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目变更记录服务实现
 *
 * <p>变更流转：PENDING → APPROVED/REJECTED → EXECUTED。
 * 申请时记录申请人（当前登录人），审批时记录审批人与审批时间。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectChangeLogServiceImpl implements ProjectChangeLogService {

    private final ProjectChangeLogMapper projectChangeLogMapper;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public List<ProjectChangeLogVO> listByProjectId(Long projectId) {
        List<ProjectChangeLogEntity> list = projectChangeLogMapper.selectByProjectId(projectId);
        return list.stream().map(ProjectConverters::toChangeLogVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyChange(ProjectChangeDTO dto) {
        ProjectChangeLogEntity entity = new ProjectChangeLogEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setChangeType(StringUtils.hasText(dto.getChangeType())
                ? dto.getChangeType() : ProjectConstant.CHANGE_TYPE_OTHER);
        entity.setChangeContent(dto.getChangeContent());
        entity.setReason(dto.getReason());
        entity.setImpactAnalysis(dto.getImpactAnalysis());
        entity.setStatus(ProjectConstant.CHANGE_PENDING);
        entity.setApplicantId(UserContextHolder.getUserId());
        projectChangeLogMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ChangeApproveDTO dto) {
        ProjectChangeLogEntity exist = projectChangeLogMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "变更记录不存在");
        }
        if (!ProjectConstant.CHANGE_PENDING.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("仅待审批变更可执行审批");
        }
        String result = dto.getApproveResult();
        if (ProjectConstant.CHANGE_APPROVED.equals(result)) {
            exist.setStatus(ProjectConstant.CHANGE_APPROVED);
        } else if (ProjectConstant.CHANGE_REJECTED.equals(result)) {
            exist.setStatus(ProjectConstant.CHANGE_REJECTED);
        } else {
            throw new BusinessException(ResultCode.PARAM_INVALID, "审批结果只能为 APPROVED 或 REJECTED");
        }
        exist.setApproverId(UserContextHolder.getUserId());
        exist.setApproveTime(LocalDateTime.now());
        // 审批意见追加到影响评估后（留痕）
        if (StringUtils.hasText(dto.getOpinion())) {
            String origin = exist.getImpactAnalysis();
            exist.setImpactAnalysis((origin == null ? "" : origin + "\n")
                    + "【审批意见】" + dto.getOpinion());
        }
        projectChangeLogMapper.updateById(exist);

        // 变更审批通过后发布领域事件
        if (ProjectConstant.CHANGE_APPROVED.equals(result)) {
            domainEventPublisher.publish(new ChangeApprovedEvent(
                    exist.getId(), exist.getProjectId(), exist.getApproverId(),
                    exist.getChangeType(), null));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long id) {
        ProjectChangeLogEntity exist = projectChangeLogMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "变更记录不存在");
        }
        if (!ProjectConstant.CHANGE_APPROVED.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("仅已通过审批的变更可执行");
        }
        exist.setStatus(ProjectConstant.CHANGE_EXECUTED);
        projectChangeLogMapper.updateById(exist);
    }

    @Override
    public ProjectChangeLogVO getDetail(Long id) {
        ProjectChangeLogEntity entity = projectChangeLogMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "变更记录不存在");
        }
        return ProjectConverters.toChangeLogVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectChangeLogEntity exist = projectChangeLogMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "变更记录不存在");
        }
        projectChangeLogMapper.deleteById(id);
    }
}
