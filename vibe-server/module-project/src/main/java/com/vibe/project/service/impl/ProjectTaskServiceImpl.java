package com.vibe.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.event.DomainEventPublisher;
import com.vibe.event.events.TaskAssignedEvent;
import com.vibe.event.events.TaskCompletedEvent;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.BatchTaskDispatchDTO;
import com.vibe.project.dto.ProjectTaskDTO;
import com.vibe.project.dto.ProjectTaskQueryDTO;
import com.vibe.project.dto.TaskDispatchDTO;
import com.vibe.project.dto.TaskProgressDTO;
import com.vibe.project.dto.TaskReturnDTO;
import com.vibe.project.dto.TaskTransferDTO;
import com.vibe.project.entity.ProjectEntity;
import com.vibe.project.entity.ProjectTaskEntity;
import com.vibe.project.enums.TaskStatusEnum;
import com.vibe.project.mapper.ProjectMapper;
import com.vibe.project.mapper.ProjectTaskMapper;
import com.vibe.project.service.ProjectTaskService;
import com.vibe.project.vo.ProjectTaskVO;
import com.vibe.system.notification.NotificationConstant;
import com.vibe.system.notification.NotificationEvent;
import com.vibe.system.notification.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目任务服务实现
 *
 * <p>核心实现要点：</p>
 * <ul>
 *   <li>任务分解：通过 parent_task_id 建立父子关系；删除父任务前校验子任务存在性</li>
 *   <li>依赖校验：父任务计划区间需覆盖子任务计划区间；同阶段任务计划区间不可冲突（Phase 1 简化）</li>
 *   <li>派发：SELF 模式写 assignee_id；AGENT 模式写 agent_company_id/agent_engineer_id（转包任务记录由 outsource 模块负责创建）</li>
 *   <li>进度同步：子任务全部 COMPLETED/CONFIRMED 时父任务可推进；项目进度按任务完成率回写</li>
 *   <li>甘特图排期：拖拽改期时校验父子区间包含关系与时间合法性</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectTaskServiceImpl implements ProjectTaskService {

    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectMapper projectMapper;
    private final NotificationProducer notificationProducer;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public PageResult<ProjectTaskVO> page(ProjectTaskQueryDTO query) {
        IPage<ProjectTaskVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<ProjectTaskVO> result = projectTaskMapper.selectTaskPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public List<ProjectTaskVO> listByProjectId(Long projectId) {
        List<ProjectTaskEntity> list = projectTaskMapper.selectByProjectId(projectId);
        return list.stream().map(ProjectConverters::toTaskVo).collect(Collectors.toList());
    }

    @Override
    public ProjectTaskVO getDetail(Long id) {
        ProjectTaskVO vo = projectTaskMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectTaskDTO dto) {
        validateDateRange(dto.getPlannedStart(), dto.getPlannedEnd());
        // 父任务存在性校验
        if (dto.getParentTaskId() != null) {
            ProjectTaskEntity parent = projectTaskMapper.selectById(dto.getParentTaskId());
            if (parent == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "父任务不存在");
            }
            // 子任务计划区间需在父任务区间内
            validateChildRange(dto.getPlannedStart(), dto.getPlannedEnd(),
                    parent.getPlannedStart(), parent.getPlannedEnd());
        }
        ProjectTaskEntity entity = new ProjectTaskEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setPhaseId(dto.getPhaseId());
        entity.setParentTaskId(dto.getParentTaskId());
        entity.setTaskName(dto.getTaskName());
        entity.setTaskType(dto.getTaskType());
        entity.setStatus(ProjectConstant.TASK_PENDING);
        entity.setExecuteMode(dto.getExecuteMode());
        entity.setPriority(StringUtils.hasText(dto.getPriority())
                ? dto.getPriority() : ProjectConstant.TASK_PRIORITY_MEDIUM);
        entity.setPlannedStart(dto.getPlannedStart());
        entity.setPlannedEnd(dto.getPlannedEnd());
        entity.setSiteInfo(dto.getSiteInfo());
        entity.setDeviceIds(dto.getDeviceIds());
        entity.setDescription(dto.getDescription());
        entity.setAttachments(dto.getAttachments());
        projectTaskMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProjectTaskDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "任务ID不能为空");
        }
        ProjectTaskEntity exist = projectTaskMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        validateDateRange(dto.getPlannedStart(), dto.getPlannedEnd());
        if (dto.getParentTaskId() != null) {
            if (dto.getParentTaskId().equals(exist.getId())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "不能将自身设为父任务");
            }
            ProjectTaskEntity parent = projectTaskMapper.selectById(dto.getParentTaskId());
            if (parent == null) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "父任务不存在");
            }
            validateChildRange(dto.getPlannedStart(), dto.getPlannedEnd(),
                    parent.getPlannedStart(), parent.getPlannedEnd());
        }
        if (StringUtils.hasText(dto.getTaskName())) {
            exist.setTaskName(dto.getTaskName());
        }
        if (dto.getPhaseId() != null) {
            exist.setPhaseId(dto.getPhaseId());
        }
        if (dto.getParentTaskId() != null) {
            exist.setParentTaskId(dto.getParentTaskId());
        }
        if (StringUtils.hasText(dto.getTaskType())) {
            exist.setTaskType(dto.getTaskType());
        }
        if (StringUtils.hasText(dto.getExecuteMode())) {
            exist.setExecuteMode(dto.getExecuteMode());
        }
        if (StringUtils.hasText(dto.getPriority())) {
            exist.setPriority(dto.getPriority());
        }
        if (dto.getPlannedStart() != null) {
            exist.setPlannedStart(dto.getPlannedStart());
        }
        if (dto.getPlannedEnd() != null) {
            exist.setPlannedEnd(dto.getPlannedEnd());
        }
        if (dto.getSiteInfo() != null) {
            exist.setSiteInfo(dto.getSiteInfo());
        }
        if (dto.getDeviceIds() != null) {
            exist.setDeviceIds(dto.getDeviceIds());
        }
        if (dto.getDescription() != null) {
            exist.setDescription(dto.getDescription());
        }
        if (dto.getAttachments() != null) {
            exist.setAttachments(dto.getAttachments());
        }
        projectTaskMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectTaskEntity exist = projectTaskMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        // 存在子任务时不允许删除
        long childCount = projectTaskMapper.selectCount(
                new LambdaQueryWrapper<ProjectTaskEntity>()
                        .eq(ProjectTaskEntity::getParentTaskId, id));
        if (childCount > 0) {
            throw BusinessException.conflict("存在子任务，无法删除");
        }
        // 终态任务不允许删除
        if (ProjectConstant.TASK_CONFIRMED.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("已确认任务不允许删除");
        }
        projectTaskMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(Long taskId, TaskDispatchDTO dto) {
        ProjectTaskEntity exist = projectTaskMapper.selectById(taskId);
        if (exist == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        // 仅 PENDING / ASSIGNED 状态可派发/重新派发
        if (!ProjectConstant.TASK_PENDING.equals(exist.getStatus())
                && !ProjectConstant.TASK_ASSIGNED.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("当前状态不允许派发");
        }
        String mode = dto.getExecuteMode();
        if (ProjectConstant.EXECUTE_MODE_SELF.equals(mode)) {
            if (dto.getAssigneeId() == null) {
                throw new BusinessException(ResultCode.PARAM_MISSING, "SELF 模式执行人ID必填");
            }
            exist.setExecuteMode(mode);
            exist.setAssigneeId(dto.getAssigneeId());
            exist.setAgentCompanyId(null);
            exist.setAgentEngineerId(null);
        } else if (ProjectConstant.EXECUTE_MODE_AGENT.equals(mode)) {
            if (dto.getAgentCompanyId() == null) {
                throw new BusinessException(ResultCode.PARAM_MISSING, "AGENT 模式代理商公司ID必填");
            }
            exist.setExecuteMode(mode);
            exist.setAgentCompanyId(dto.getAgentCompanyId());
            exist.setAgentEngineerId(dto.getAgentEngineerId());
            // AGENT 模式下 assigneeId 置空
            exist.setAssigneeId(null);
            // 注：转包任务（outsource_task）记录由 outsource 模块负责创建与同步，
            // 此处仅维护项目任务侧的代理商字段。
            log.info("AGENT 派发: taskId={}, agentCompanyId={}, agentEngineerId={}",
                    taskId, dto.getAgentCompanyId(), dto.getAgentEngineerId());
        } else {
            throw new BusinessException(ResultCode.PARAM_INVALID, "执行模式只能为 SELF 或 AGENT");
        }
        exist.setStatus(ProjectConstant.TASK_ASSIGNED);
        projectTaskMapper.updateById(exist);

        // 通知事件投递：TASK_ASSIGNED
        sendTaskAssignedNotification(exist, mode);

        // 发布任务派发领域事件
        Long assigneeIdForEvent = ProjectConstant.EXECUTE_MODE_AGENT.equals(mode)
                ? exist.getAgentEngineerId() : exist.getAssigneeId();
        domainEventPublisher.publish(new TaskAssignedEvent(
                exist.getId(), exist.getProjectId(), assigneeIdForEvent,
                exist.getTaskName(), mode));
    }

    /**
     * 投递任务派发通知事件。
     */
    private void sendTaskAssignedNotification(ProjectTaskEntity task, String mode) {
        Long recipientId = ProjectConstant.EXECUTE_MODE_AGENT.equals(mode)
                ? task.getAgentEngineerId() : task.getAssigneeId();
        if (recipientId == null) {
            log.info("任务派发通知跳过（无接收人）: taskId={}, mode={}", task.getId(), mode);
            return;
        }
        String recipientType = ProjectConstant.EXECUTE_MODE_AGENT.equals(mode)
                ? NotificationConstant.RECIPIENT_AGENT
                : NotificationConstant.RECIPIENT_INTERNAL;
        // 查询项目名称（projectMapper 已注入）
        String projectName = "";
        ProjectEntity project = projectMapper.selectById(task.getProjectId());
        if (project != null) {
            projectName = project.getProjectName() == null ? "" : project.getProjectName();
        }
        Map<String, String> variables = new HashMap<>(4);
        variables.put("taskName", task.getTaskName() == null ? "" : task.getTaskName());
        variables.put("projectName", projectName);
        variables.put("plannedStart", task.getPlannedStart() == null ? "" : task.getPlannedStart().toString());
        variables.put("plannedEnd", task.getPlannedEnd() == null ? "" : task.getPlannedEnd().toString());
        NotificationEvent event = NotificationEvent.of(
                NotificationConstant.EVENT_TASK_ASSIGNED, recipientType,
                Collections.singletonList(recipientId), variables,
                task.getId(), NotificationConstant.BIZ_PROJECT_TASK);
        notificationProducer.send(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDispatch(BatchTaskDispatchDTO dto) {
        if (dto.getDispatch() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "派发信息不能为空");
        }
        int success = 0;
        for (Long taskId : dto.getTaskIds()) {
            try {
                dispatch(taskId, dto.getDispatch());
                success++;
            } catch (BusinessException e) {
                log.warn("批量派单失败: taskId={}, code={}, msg={}",
                        taskId, e.getCode(), e.getMessage());
            }
        }
        if (success == 0) {
            throw BusinessException.conflict("批量派单全部失败");
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long taskId, TaskTransferDTO dto) {
        ProjectTaskEntity exist = projectTaskMapper.selectById(taskId);
        if (exist == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        // 仅 ASSIGNED / IN_PROGRESS 状态可转派
        if (!ProjectConstant.TASK_ASSIGNED.equals(exist.getStatus())
                && !ProjectConstant.TASK_IN_PROGRESS.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("当前状态不允许转派");
        }
        if (dto.getNewAssigneeId() != null) {
            exist.setExecuteMode(ProjectConstant.EXECUTE_MODE_SELF);
            exist.setAssigneeId(dto.getNewAssigneeId());
            exist.setAgentCompanyId(null);
            exist.setAgentEngineerId(null);
        } else if (dto.getNewAgentCompanyId() != null) {
            exist.setExecuteMode(ProjectConstant.EXECUTE_MODE_AGENT);
            exist.setAgentCompanyId(dto.getNewAgentCompanyId());
            exist.setAgentEngineerId(dto.getNewAgentEngineerId());
            exist.setAssigneeId(null);
        } else {
            throw new BusinessException(ResultCode.PARAM_MISSING, "需指定新执行人或新代理商");
        }
        // 转派后状态回到 ASSIGNED
        exist.setStatus(ProjectConstant.TASK_ASSIGNED);
        projectTaskMapper.updateById(exist);
        log.info("任务转派: taskId={}, reason={}", taskId, dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(Long taskId, TaskReturnDTO dto) {
        ProjectTaskEntity exist = projectTaskMapper.selectById(taskId);
        if (exist == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        // 仅 ASSIGNED / IN_PROGRESS 状态可退回
        TaskStatusEnum current = TaskStatusEnum.of(exist.getStatus());
        if (current != TaskStatusEnum.ASSIGNED && current != TaskStatusEnum.IN_PROGRESS) {
            throw BusinessException.stateNotAllowed("当前状态不允许退回");
        }
        exist.setStatus(ProjectConstant.TASK_PENDING);
        exist.setAssigneeId(null);
        exist.setAgentCompanyId(null);
        exist.setAgentEngineerId(null);
        // 退回清空实际开始时间
        exist.setActualStart(null);
        projectTaskMapper.updateById(exist);
        log.info("任务退回: taskId={}, reason={}", taskId, dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long taskId, TaskProgressDTO dto) {
        ProjectTaskEntity exist = projectTaskMapper.selectById(taskId);
        if (exist == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        TaskStatusEnum current = TaskStatusEnum.of(exist.getStatus());
        TaskStatusEnum target = TaskStatusEnum.of(dto.getTargetStatus());
        if (current == null || target == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "无效的任务状态");
        }
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    String.format("任务状态流转非法：%s → %s", current.getDesc(), target.getDesc()));
        }
        exist.setStatus(target.getCode());
        // IN_PROGRESS 时记录实际开始
        if (target == TaskStatusEnum.IN_PROGRESS && exist.getActualStart() == null) {
            exist.setActualStart(LocalDate.now());
        }
        // COMPLETED 时记录实际结束
        if (target == TaskStatusEnum.COMPLETED && exist.getActualEnd() == null) {
            exist.setActualEnd(LocalDate.now());
        }
        // 退回至 IN_PROGRESS 时清空实际结束
        if (target == TaskStatusEnum.IN_PROGRESS && current == TaskStatusEnum.COMPLETED) {
            exist.setActualEnd(null);
        }
        int rows = projectTaskMapper.updateById(exist);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    "任务已被其他操作更新，请刷新后重试");
        }
        // 触发进度同步：父任务与项目
        if (exist.getParentTaskId() != null) {
            syncParentProgress(exist.getParentTaskId());
        }
        syncProjectProgress(exist.getProjectId());

        // 任务流转至 COMPLETED 时发布任务完成领域事件
        if (target == TaskStatusEnum.COMPLETED) {
            domainEventPublisher.publish(new TaskCompletedEvent(
                    exist.getId(), exist.getProjectId(),
                    exist.getAssigneeId(), null));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncProgress(Long projectId) {
        syncProjectProgress(projectId);
        // 同步项目下所有有子任务的父任务进度
        List<ProjectTaskEntity> all = projectTaskMapper.selectByProjectId(projectId);
        for (ProjectTaskEntity task : all) {
            if (task.getParentTaskId() != null) {
                continue;
            }
            // 检查是否有子任务
            long childCount = projectTaskMapper.selectCount(
                    new LambdaQueryWrapper<ProjectTaskEntity>()
                            .eq(ProjectTaskEntity::getParentTaskId, task.getId()));
            if (childCount > 0) {
                syncParentProgress(task.getId());
            }
        }
    }

    @Override
    public List<ProjectTaskVO> listOverdueTasks() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<ProjectTaskEntity> wrapper = new LambdaQueryWrapper<ProjectTaskEntity>()
                .lt(ProjectTaskEntity::getPlannedEnd, today)
                .notIn(ProjectTaskEntity::getStatus,
                        ProjectConstant.TASK_COMPLETED, ProjectConstant.TASK_CONFIRMED)
                .isNotNull(ProjectTaskEntity::getPlannedEnd);
        List<ProjectTaskEntity> list = projectTaskMapper.selectList(wrapper);
        return list.stream().map(ProjectConverters::toTaskVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reschedule(Long taskId, LocalDate newStart, LocalDate newEnd) {
        validateDateRange(newStart, newEnd);
        ProjectTaskEntity exist = projectTaskMapper.selectById(taskId);
        if (exist == null) {
            throw BusinessException.of(ResultCode.TASK_NOT_FOUND);
        }
        // 终态任务不允许改期
        if (ProjectConstant.TASK_CONFIRMED.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("已确认任务不允许改期");
        }
        // 依赖冲突检测：父任务区间需覆盖新区间
        if (exist.getParentTaskId() != null) {
            ProjectTaskEntity parent = projectTaskMapper.selectById(exist.getParentTaskId());
            if (parent != null) {
                validateChildRange(newStart, newEnd,
                        parent.getPlannedStart(), parent.getPlannedEnd());
            }
        }
        // 子任务区间需被新区间覆盖
        List<ProjectTaskEntity> children = projectTaskMapper.selectList(
                new LambdaQueryWrapper<ProjectTaskEntity>()
                        .eq(ProjectTaskEntity::getParentTaskId, taskId));
        for (ProjectTaskEntity child : children) {
            if (child.getPlannedStart() != null && newStart != null
                    && child.getPlannedStart().isBefore(newStart)) {
                throw new BusinessException(ResultCode.DEPENDENCY_CONFLICT,
                        "子任务 " + child.getTaskName() + " 的计划开始早于父任务新开始时间");
            }
            if (child.getPlannedEnd() != null && newEnd != null
                    && child.getPlannedEnd().isAfter(newEnd)) {
                throw new BusinessException(ResultCode.DEPENDENCY_CONFLICT,
                        "子任务 " + child.getTaskName() + " 的计划结束晚于父任务新结束时间");
            }
        }
        exist.setPlannedStart(newStart);
        exist.setPlannedEnd(newEnd);
        projectTaskMapper.updateById(exist);
    }

    /* ============ 私有方法 ============ */

    /**
     * 校验时间范围：开始不能晚于结束
     */
    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "计划开始时间不能晚于计划结束时间");
        }
    }

    /**
     * 校验子任务区间需在父任务区间内
     */
    private void validateChildRange(LocalDate childStart, LocalDate childEnd,
                                    LocalDate parentStart, LocalDate parentEnd) {
        if (childStart == null || childEnd == null || parentStart == null || parentEnd == null) {
            return;
        }
        if (childStart.isBefore(parentStart)) {
            throw new BusinessException(ResultCode.DEPENDENCY_CONFLICT,
                    "子任务计划开始不能早于父任务计划开始");
        }
        if (childEnd.isAfter(parentEnd)) {
            throw new BusinessException(ResultCode.DEPENDENCY_CONFLICT,
                    "子任务计划结束不能晚于父任务计划结束");
        }
    }

    /**
     * 同步父任务进度：所有子任务 CONFIRMED 时父任务自动推进到 COMPLETED；
     * 所有子任务 COMPLETED/CONFIRMED 时父任务可推进到 COMPLETED。
     */
    private void syncParentProgress(Long parentId) {
        ProjectTaskEntity parent = projectTaskMapper.selectById(parentId);
        if (parent == null) {
            return;
        }
        if (ProjectConstant.TASK_CONFIRMED.equals(parent.getStatus())) {
            return;
        }
        List<ProjectTaskEntity> children = projectTaskMapper.selectList(
                new LambdaQueryWrapper<ProjectTaskEntity>()
                        .eq(ProjectTaskEntity::getParentTaskId, parentId));
        if (children.isEmpty()) {
            return;
        }
        boolean allCompleted = children.stream().allMatch(c ->
                ProjectConstant.TASK_COMPLETED.equals(c.getStatus())
                        || ProjectConstant.TASK_CONFIRMED.equals(c.getStatus()));
        if (allCompleted && (ProjectConstant.TASK_IN_PROGRESS.equals(parent.getStatus())
                || ProjectConstant.TASK_ASSIGNED.equals(parent.getStatus()))) {
            parent.setStatus(ProjectConstant.TASK_COMPLETED);
            if (parent.getActualEnd() == null) {
                parent.setActualEnd(LocalDate.now());
            }
            projectTaskMapper.updateById(parent);
            log.info("父任务自动完成同步: parentId={}", parentId);
        }
    }

    /**
     * 同步项目进度：项目进度 = 已完成任务数 / 总任务数 * 100
     */
    private void syncProjectProgress(Long projectId) {
        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        List<ProjectTaskEntity> tasks = projectTaskMapper.selectByProjectId(projectId);
        if (tasks.isEmpty()) {
            return;
        }
        long completed = tasks.stream().filter(t ->
                ProjectConstant.TASK_COMPLETED.equals(t.getStatus())
                        || ProjectConstant.TASK_CONFIRMED.equals(t.getStatus())).count();
        int pct = (int) (completed * 100 / tasks.size());
        // 仅当进度变化时更新，减少无谓写库
        if (project.getProgressPct() == null || project.getProgressPct() != pct) {
            project.setProgressPct(pct);
            projectMapper.updateById(project);
            log.info("项目进度同步: projectId={}, progressPct={}%", projectId, pct);
        }
    }
}
