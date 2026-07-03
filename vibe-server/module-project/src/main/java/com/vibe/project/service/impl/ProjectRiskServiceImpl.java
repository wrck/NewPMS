package com.vibe.project.service.impl;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.ProjectRiskDTO;
import com.vibe.project.entity.ProjectEntity;
import com.vibe.project.entity.ProjectRiskEntity;
import com.vibe.project.mapper.ProjectMapper;
import com.vibe.project.mapper.ProjectRiskMapper;
import com.vibe.project.service.ProjectRiskService;
import com.vibe.project.vo.ProjectRiskVO;
import com.vibe.system.notification.NotificationConstant;
import com.vibe.system.notification.NotificationEvent;
import com.vibe.system.notification.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目风险服务实现
 *
 * <p>状态流转：OPEN → PROCESSING → RESOLVED → CLOSED，
 * 超期未关闭（dueDate 早于今天且状态非 RESOLVED/CLOSED）由定时任务升级为高优先级提醒。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRiskServiceImpl implements ProjectRiskService {

    private final ProjectRiskMapper projectRiskMapper;
    private final ProjectMapper projectMapper;
    private final NotificationProducer notificationProducer;

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
    public List<ProjectRiskVO> listByProjectId(Long projectId) {
        List<ProjectRiskEntity> list = projectRiskMapper.selectByProjectId(projectId);
        return list.stream().map(ProjectConverters::toRiskVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectRiskDTO dto) {
        ProjectRiskEntity entity = new ProjectRiskEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setRiskDesc(dto.getRiskDesc());
        entity.setImpact(dto.getImpact());
        entity.setProbability(dto.getProbability());
        entity.setMeasure(dto.getMeasure());
        entity.setOwnerId(dto.getOwnerId());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : ProjectConstant.TRACK_OPEN);
        entity.setDueDate(dto.getDueDate());
        projectRiskMapper.insert(entity);

        // 通知事件投递：RISK_WARNING（通知风险责任人/PM）
        sendRiskWarningNotification(entity);
        return entity.getId();
    }

    /**
     * 投递项目风险预警通知。
     *
     * <p>接收人：风险责任人 ownerId（如有），同时通过飞书群触达 PM 群。</p>
     */
    private void sendRiskWarningNotification(ProjectRiskEntity risk) {
        String projectName = "";
        if (risk.getProjectId() != null) {
            ProjectEntity project = projectMapper.selectById(risk.getProjectId());
            if (project != null && project.getProjectName() != null) {
                projectName = project.getProjectName();
            }
        }
        Map<String, String> variables = new HashMap<>(4);
        variables.put("projectName", projectName);
        variables.put("riskDesc", risk.getRiskDesc() == null ? "" : risk.getRiskDesc());
        variables.put("impact", risk.getImpact() == null ? "" : risk.getImpact());
        variables.put("probability", risk.getProbability() == null ? "" : risk.getProbability());
        List<Long> recipientIds = risk.getOwnerId() == null
                ? Collections.emptyList()
                : Collections.singletonList(risk.getOwnerId());
        NotificationEvent event = NotificationEvent.of(
                NotificationConstant.EVENT_RISK_WARNING,
                NotificationConstant.RECIPIENT_INTERNAL,
                recipientIds, variables,
                risk.getId(), NotificationConstant.BIZ_PROJECT_RISK);
        notificationProducer.send(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProjectRiskDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "风险ID不能为空");
        }
        ProjectRiskEntity exist = projectRiskMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "风险不存在");
        }
        if (StringUtils.hasText(dto.getRiskDesc())) {
            exist.setRiskDesc(dto.getRiskDesc());
        }
        if (StringUtils.hasText(dto.getImpact())) {
            exist.setImpact(dto.getImpact());
        }
        if (StringUtils.hasText(dto.getProbability())) {
            exist.setProbability(dto.getProbability());
        }
        if (dto.getMeasure() != null) {
            exist.setMeasure(dto.getMeasure());
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
        projectRiskMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectRiskEntity exist = projectRiskMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "风险不存在");
        }
        projectRiskMapper.deleteById(id);
    }

    @Override
    public ProjectRiskVO getDetail(Long id) {
        ProjectRiskEntity entity = projectRiskMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "风险不存在");
        }
        return ProjectConverters.toRiskVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long id, String targetStatus) {
        ProjectRiskEntity exist = projectRiskMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "风险不存在");
        }
        String current = exist.getStatus();
        Set<String> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    String.format("风险状态流转非法：%s → %s", current, targetStatus));
        }
        exist.setStatus(targetStatus);
        projectRiskMapper.updateById(exist);
    }

    @Override
    public List<ProjectRiskVO> listOverdue() {
        List<ProjectRiskEntity> list = projectRiskMapper.selectOverdueRisks(LocalDate.now());
        return list.stream().map(ProjectConverters::toRiskVo).collect(Collectors.toList());
    }
}
