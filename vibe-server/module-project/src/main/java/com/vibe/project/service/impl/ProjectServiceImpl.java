package com.vibe.project.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.constant.CommonConstant;
import com.vibe.common.constant.RedisKeyConstant;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.event.DomainEventPublisher;
import com.vibe.event.events.ProjectCreatedEvent;
import com.vibe.event.events.ProjectStatusChangedEvent;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.converter.ProjectConverters;
import com.vibe.project.dto.ProjectArchiveDTO;
import com.vibe.project.dto.ProjectCreateDTO;
import com.vibe.project.dto.ProjectQueryDTO;
import com.vibe.project.dto.ProjectStatusDTO;
import com.vibe.project.dto.ProjectUpdateDTO;
import com.vibe.project.entity.ProjectEntity;
import com.vibe.project.entity.ProjectMilestoneEntity;
import com.vibe.project.entity.ProjectPhaseEntity;
import com.vibe.project.entity.ProjectTemplateEntity;
import com.vibe.project.entity.ProjectTemplatePhaseEntity;
import com.vibe.project.entity.ProjectTemplateTaskEntity;
import com.vibe.project.enums.ProjectStatusEnum;
import com.vibe.project.mapper.ProjectMapper;
import com.vibe.project.mapper.ProjectMilestoneMapper;
import com.vibe.project.mapper.ProjectPhaseMapper;
import com.vibe.project.mapper.ProjectRiskMapper;
import com.vibe.project.mapper.ProjectIssueMapper;
import com.vibe.project.mapper.ProjectTaskMapper;
import com.vibe.project.mapper.ProjectMemberMapper;
import com.vibe.project.mapper.ProjectTemplateMapper;
import com.vibe.project.mapper.ProjectTemplatePhaseMapper;
import com.vibe.project.mapper.ProjectTemplateTaskMapper;
import com.vibe.project.service.ProjectService;
import com.vibe.project.vo.ProjectDetailVO;
import com.vibe.project.vo.ProjectGanttVO;
import com.vibe.project.vo.ProjectKanbanVO;
import com.vibe.project.vo.ProjectMemberVO;
import com.vibe.project.vo.ProjectMilestoneVO;
import com.vibe.project.vo.ProjectPhaseVO;
import com.vibe.project.vo.ProjectVO;
import com.vibe.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目服务实现
 *
 * <p>核心业务：</p>
 * <ul>
 *   <li>立项：手动创建或选择模板自动生成阶段与任务，项目编号 PRJ-YYYYMM-XXX 由 Redis INCR 生成</li>
 *   <li>状态机：INIT→PLAN→EXECUTE→ACCEPT→CLOSE→ARCHIVED + ON_HOLD/CANCELLED，流转校验 + 乐观锁</li>
 *   <li>查询：分页、看板分组、甘特图、详情聚合</li>
 *   <li>结项检查：所有任务 COMPLETED/CONFIRMED 才允许 ACCEPT→CLOSE</li>
 *   <li>归档：CLOSE→ARCHIVED，记录复盘与经验沉淀</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectPhaseMapper projectPhaseMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectMilestoneMapper projectMilestoneMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectRiskMapper projectRiskMapper;
    private final ProjectIssueMapper projectIssueMapper;
    private final ProjectTemplateMapper projectTemplateMapper;
    private final ProjectTemplatePhaseMapper projectTemplatePhaseMapper;
    private final ProjectTemplateTaskMapper projectTemplateTaskMapper;
    private final RedisUtils redisUtils;
    private final DomainEventPublisher domainEventPublisher;

    /** 项目编号序号格式化（3位补零） */
    private static final String SEQ_FORMAT = "%03d";
    /** 月份格式化 */
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateDTO dto) {
        ProjectEntity entity = new ProjectEntity();
        // 生成项目编号 PRJ-YYYYMM-XXX
        entity.setProjectCode(generateProjectCode());
        entity.setProjectName(dto.getProjectName());
        entity.setCustomerId(dto.getCustomerId());
        entity.setProjectType(dto.getProjectType());
        entity.setProductLine(dto.getProductLine());
        entity.setExecuteMode(StringUtils.hasText(dto.getExecuteMode())
                ? dto.getExecuteMode() : ProjectConstant.EXECUTE_MODE_SELF);
        entity.setPriority(StringUtils.hasText(dto.getPriority())
                ? dto.getPriority() : ProjectConstant.PRIORITY_P2);
        entity.setStatus(ProjectConstant.STATUS_INIT);
        entity.setPmId(dto.getPmId());
        entity.setRegion(dto.getRegion());
        entity.setContractNo(dto.getContractNo());
        entity.setPlannedStart(dto.getPlannedStart());
        entity.setPlannedEnd(dto.getPlannedEnd());
        entity.setProgressPct(0);
        entity.setDescription(dto.getDescription());
        entity.setRemark(dto.getRemark());
        projectMapper.insert(entity);

        // 选择模板时自动生成阶段与任务
        if (dto.getTemplateId() != null) {
            applyTemplate(entity.getId(), dto.getTemplateId(), dto.getPlannedStart());
        }

        // PM 自动加入项目成员
        if (dto.getPmId() != null) {
            addMember(entity.getId(), dto.getPmId(), ProjectConstant.MEMBER_ROLE_PM);
        }

        // 发布项目立项领域事件
        domainEventPublisher.publish(new ProjectCreatedEvent(
                entity.getId(), entity.getProjectName(), entity.getPmId(), null));

        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProjectUpdateDTO dto) {
        ProjectEntity exist = projectMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        // 仅 INIT/PLAN/EXECUTE/ON_HOLD 状态可编辑
        if (!isEditable(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("当前状态不允许编辑项目信息");
        }
        if (StringUtils.hasText(dto.getProjectName())) {
            exist.setProjectName(dto.getProjectName());
        }
        if (dto.getCustomerId() != null) {
            exist.setCustomerId(dto.getCustomerId());
        }
        if (StringUtils.hasText(dto.getProjectType())) {
            exist.setProjectType(dto.getProjectType());
        }
        if (StringUtils.hasText(dto.getProductLine())) {
            exist.setProductLine(dto.getProductLine());
        }
        if (StringUtils.hasText(dto.getExecuteMode())) {
            exist.setExecuteMode(dto.getExecuteMode());
        }
        if (StringUtils.hasText(dto.getPriority())) {
            exist.setPriority(dto.getPriority());
        }
        if (dto.getPmId() != null) {
            exist.setPmId(dto.getPmId());
        }
        if (StringUtils.hasText(dto.getRegion())) {
            exist.setRegion(dto.getRegion());
        }
        if (dto.getContractNo() != null) {
            exist.setContractNo(dto.getContractNo());
        }
        if (dto.getPlannedStart() != null) {
            exist.setPlannedStart(dto.getPlannedStart());
        }
        if (dto.getPlannedEnd() != null) {
            exist.setPlannedEnd(dto.getPlannedEnd());
        }
        if (dto.getDescription() != null) {
            exist.setDescription(dto.getDescription());
        }
        if (dto.getRemark() != null) {
            exist.setRemark(dto.getRemark());
        }
        projectMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectEntity exist = projectMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        // 仅 INIT/PLAN 状态可删除（防止误删已执行项目）
        if (!ProjectConstant.STATUS_INIT.equals(exist.getStatus())
                && !ProjectConstant.STATUS_PLAN.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("仅立项/规划中状态的项目可删除");
        }
        projectMapper.deleteById(id);
    }

    @Override
    public PageResult<ProjectVO> page(ProjectQueryDTO query) {
        IPage<ProjectVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<ProjectVO> result = projectMapper.selectProjectPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public ProjectDetailVO getDetail(Long id) {
        ProjectVO base = projectMapper.selectVoById(id);
        if (base == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        ProjectDetailVO vo = ProjectConverters.toProjectDetailVo(projectMapper.selectById(id));

        // 阶段列表
        List<ProjectPhaseEntity> phases = projectPhaseMapper.selectByProjectId(id);
        vo.setPhases(phases.stream().map(ProjectConverters::toPhaseVo).collect(Collectors.toList()));

        // 里程碑
        List<ProjectMilestoneEntity> milestones = projectMilestoneMapper.selectByProjectId(id);
        vo.setMilestones(milestones.stream().map(ProjectConverters::toMilestoneVo)
                .collect(Collectors.toList()));

        // 成员列表
        List<com.vibe.project.entity.ProjectMemberEntity> members =
                projectMemberMapper.selectByProjectId(id);
        vo.setMembers(members.stream().map(ProjectConverters::toMemberVo)
                .collect(Collectors.toList()));

        // 任务统计
        List<Map<String, Object>> statusCounts = projectTaskMapper.selectStatusCountByProject(id);
        int total = 0;
        int completed = 0;
        int inProgress = 0;
        int pending = 0;
        if (statusCounts != null) {
            for (Map<String, Object> sc : statusCounts) {
                String status = (String) sc.get("status");
                int cnt = ((Number) sc.get("cnt")).intValue();
                total += cnt;
                if (ProjectConstant.TASK_COMPLETED.equals(status)
                        || ProjectConstant.TASK_CONFIRMED.equals(status)) {
                    completed += cnt;
                } else if (ProjectConstant.TASK_IN_PROGRESS.equals(status)) {
                    inProgress += cnt;
                } else if (ProjectConstant.TASK_PENDING.equals(status)
                        || ProjectConstant.TASK_ASSIGNED.equals(status)) {
                    pending += cnt;
                }
            }
        }
        vo.setTaskTotal(total);
        vo.setTaskCompleted(completed);
        vo.setTaskInProgress(inProgress);
        vo.setTaskPending(pending);

        // 风险/问题计数
        vo.setRiskCount(projectRiskMapper.selectByProjectId(id).size());
        vo.setIssueCount(projectIssueMapper.selectByProjectId(id).size());

        // 回填关联名称
        vo.setCustomerName(base.getCustomerName());
        vo.setPmName(base.getPmName());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transition(ProjectStatusDTO dto) {
        ProjectEntity exist = projectMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        ProjectStatusEnum current = ProjectStatusEnum.of(exist.getStatus());
        ProjectStatusEnum target = ProjectStatusEnum.of(dto.getTargetStatus());
        if (current == null || target == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "无效的项目状态");
        }
        // 状态流转校验
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    String.format("项目状态流转非法：%s → %s", current.getDesc(), target.getDesc()));
        }
        // 特定流转的业务前置校验
        checkTransitionPrecondition(exist, current, target);

        // 乐观锁校验：传入 version 时校验一致
        if (dto.getVersion() != null && !dto.getVersion().equals(exist.getVersion())) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    "项目已被其他操作更新，请刷新后重试");
        }

        exist.setStatus(target.getCode());
        // 特定流转更新实际开始/结束日期
        if (target == ProjectStatusEnum.EXECUTE && exist.getActualStart() == null) {
            exist.setActualStart(LocalDate.now());
        }
        if (target == ProjectStatusEnum.CLOSE && exist.getActualEnd() == null) {
            exist.setActualEnd(LocalDate.now());
        }
        int rows = projectMapper.updateById(exist);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    "项目状态更新失败，可能已被并发修改");
        }

        // 发布项目状态变更领域事件
        domainEventPublisher.publish(new ProjectStatusChangedEvent(
                exist.getId(), current.getCode(), target.getCode(), target.getCode()));
    }

    @Override
    public List<ProjectKanbanVO> kanban(ProjectQueryDTO query) {
        List<Map<String, Object>> rows = projectMapper.selectKanbanGroups(query);
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        // 按状态分组聚合
        Map<String, ProjectKanbanVO> grouped = new LinkedHashMap<>();
        for (ProjectStatusEnum status : ProjectStatusEnum.values()) {
            ProjectKanbanVO vo = new ProjectKanbanVO();
            vo.setStatus(status.getCode());
            vo.setStatusName(status.getDesc());
            vo.setCount(0);
            vo.setProjects(new ArrayList<>());
            grouped.put(status.getCode(), vo);
        }
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            ProjectKanbanVO vo = grouped.get(status);
            if (vo == null) {
                vo = new ProjectKanbanVO();
                vo.setStatus(status);
                vo.setStatusName(status);
                vo.setProjects(new ArrayList<>());
                grouped.put(status, vo);
            }
            ProjectVO p = new ProjectVO();
            p.setId(toLong(row.get("id")));
            p.setProjectCode((String) row.get("projectCode"));
            p.setProjectName((String) row.get("projectName"));
            p.setPriority((String) row.get("priority"));
            p.setProgressPct(toInt(row.get("progressPct")));
            p.setPmId(toLong(row.get("pmId")));
            p.setPmName((String) row.get("pmName"));
            p.setCustomerId(toLong(row.get("customerId")));
            p.setCustomerName((String) row.get("customerName"));
            p.setExecuteMode((String) row.get("executeMode"));
            p.setStatus(status);
            vo.getProjects().add(p);
            vo.setCount(vo.getCount() + 1);
        }
        return grouped.values().stream()
                .filter(v -> v.getCount() > 0)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectGanttVO gantt(Long id) {
        ProjectGanttVO vo = projectMapper.selectGanttById(id);
        if (vo == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        vo.setPhases(projectMapper.selectGanttPhases(id));
        vo.setTasks(projectMapper.selectGanttTasks(id));
        return vo;
    }

    @Override
    public String checkClose(Long id) {
        ProjectEntity exist = projectMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        // 检查是否有未完成任务
        int unfinished = projectTaskMapper.countUnfinishedByProject(id);
        if (unfinished > 0) {
            return "存在 " + unfinished + " 个未完成任务，无法结项";
        }
        // Phase 1 简化：不强制校验文档归档与费用结算
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id, ProjectArchiveDTO dto) {
        ProjectEntity exist = projectMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.PROJECT_NOT_FOUND);
        }
        ProjectStatusEnum current = ProjectStatusEnum.of(exist.getStatus());
        if (current != ProjectStatusEnum.CLOSE) {
            throw BusinessException.stateNotAllowed("仅已结项项目可归档");
        }
        exist.setStatus(ProjectConstant.STATUS_ARCHIVED);
        // 复盘记录写入 remark（Phase 1 简化：扩展 remark 字段）
        if (dto != null) {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.hasText(exist.getRemark())) {
                sb.append(exist.getRemark()).append("\n");
            }
            if (StringUtils.hasText(dto.getReviewSummary())) {
                sb.append("【复盘记录】").append(dto.getReviewSummary()).append("\n");
            }
            if (StringUtils.hasText(dto.getLessonsLearned())) {
                sb.append("【经验沉淀】").append(dto.getLessonsLearned());
            }
            exist.setRemark(sb.toString());
        }
        projectMapper.updateById(exist);
    }

    /* ============ 私有方法 ============ */

    /**
     * 生成项目编号 PRJ-YYYYMM-XXX（Redis INCR 当月序号）
     */
    private String generateProjectCode() {
        String yyyyMM = LocalDate.now().format(MONTH_FMT);
        String key = RedisKeyConstant.projectCodeSeq(yyyyMM);
        Long seq = redisUtils.increment(key);
        // 当月序号首次生成时设置过期（2个月）
        if (seq != null && seq == 1L) {
            redisUtils.expire(key, java.time.Duration.ofDays(60));
        }
        return CommonConstant.PROJECT_CODE_PREFIX + CommonConstant.DASH + yyyyMM
                + CommonConstant.DASH + String.format(SEQ_FORMAT, seq == null ? 1L : seq);
    }

    /**
     * 应用模板：根据模板阶段与任务生成项目的阶段与任务
     */
    private void applyTemplate(Long projectId, Long templateId, LocalDate plannedStart) {
        ProjectTemplateEntity template = projectTemplateMapper.selectById(templateId);
        if (template == null) {
            log.warn("项目模板不存在: templateId={}, 跳过模板生成", templateId);
            return;
        }
        List<ProjectTemplatePhaseEntity> tplPhases = projectTemplatePhaseMapper.selectByTemplateId(templateId);
        List<ProjectTemplateTaskEntity> tplTasks = projectTemplateTaskMapper.selectByTemplateId(templateId);

        // 阶段编码 → 阶段ID 映射
        Map<String, Long> phaseCodeToId = new HashMap<>();
        LocalDate cursor = plannedStart == null ? LocalDate.now() : plannedStart;
        int order = 0;
        for (ProjectTemplatePhaseEntity tplPhase : tplPhases) {
            ProjectPhaseEntity phase = new ProjectPhaseEntity();
            phase.setProjectId(projectId);
            phase.setPhaseCode(tplPhase.getPhaseCode());
            phase.setPhaseName(tplPhase.getPhaseName());
            phase.setSortOrder(tplPhase.getSortOrder() != null ? tplPhase.getSortOrder() : order++);
            phase.setStatus(ProjectConstant.PHASE_NOT_STARTED);
            phase.setDeliverables(tplPhase.getDeliverables());
            projectPhaseMapper.insert(phase);
            phaseCodeToId.put(tplPhase.getPhaseCode(), phase.getId());
        }

        // 生成任务
        for (ProjectTemplateTaskEntity tplTask : tplTasks) {
            com.vibe.project.entity.ProjectTaskEntity task = new com.vibe.project.entity.ProjectTaskEntity();
            task.setProjectId(projectId);
            task.setPhaseId(phaseCodeToId.get(tplTask.getPhaseCode()));
            task.setTaskName(tplTask.getTaskName());
            task.setTaskType(tplTask.getTaskType());
            task.setStatus(ProjectConstant.TASK_PENDING);
            task.setPriority(ProjectConstant.TASK_PRIORITY_MEDIUM);
            task.setDescription(tplTask.getDescription());
            projectTaskMapper.insert(task);
        }
    }

    /**
     * 状态流转前置条件校验
     */
    private void checkTransitionPrecondition(ProjectEntity project, ProjectStatusEnum current,
                                             ProjectStatusEnum target) {
        // EXECUTE → ACCEPT：所有实施任务完成
        if (current == ProjectStatusEnum.EXECUTE && target == ProjectStatusEnum.ACCEPT) {
            int unfinished = projectTaskMapper.countUnfinishedByProject(project.getId());
            if (unfinished > 0) {
                throw new BusinessException(ResultCode.STATE_NOT_ALLOWED,
                        "存在 " + unfinished + " 个未完成任务，无法进入验收阶段");
            }
        }
        // ACCEPT → CLOSE：结项检查
        if (current == ProjectStatusEnum.ACCEPT && target == ProjectStatusEnum.CLOSE) {
            String reason = checkClose(project.getId());
            if (reason != null) {
                throw new BusinessException(ResultCode.STATE_NOT_ALLOWED, reason);
            }
        }
    }

    /**
     * 是否可编辑
     */
    private boolean isEditable(String status) {
        return ProjectConstant.STATUS_INIT.equals(status)
                || ProjectConstant.STATUS_PLAN.equals(status)
                || ProjectConstant.STATUS_EXECUTE.equals(status)
                || ProjectConstant.STATUS_ON_HOLD.equals(status);
    }

    /**
     * 添加项目成员
     */
    private void addMember(Long projectId, Long userId, String role) {
        com.vibe.project.entity.ProjectMemberEntity member =
                new com.vibe.project.entity.ProjectMemberEntity();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinTime(java.time.LocalDateTime.now());
        try {
            projectMemberMapper.insert(member);
        } catch (Throwable e) {
            // 唯一键冲突（已存在）忽略
            log.debug("项目成员已存在: projectId={}, userId={}", projectId, userId);
        }
    }

    private Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return Long.valueOf(o.toString());
    }

    private Integer toInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.valueOf(o.toString());
    }
}
