package com.vibe.resource.service.impl;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.resource.constant.ResourceConstant;
import com.vibe.resource.dto.BatchDispatchDTO;
import com.vibe.resource.dto.TaskDispatchDTO;
import com.vibe.resource.dto.TaskRecommendationQueryDTO;
import com.vibe.resource.dto.TaskReassignDTO;
import com.vibe.resource.dto.TaskReturnDTO;
import com.vibe.resource.entity.EngineerEntity;
import com.vibe.resource.entity.EngineerScheduleEntity;
import com.vibe.resource.entity.EngineerSkillEntity;
import com.vibe.resource.mapper.EngineerMapper;
import com.vibe.resource.mapper.EngineerScheduleMapper;
import com.vibe.resource.mapper.EngineerSkillMapper;
import com.vibe.resource.mapper.ProjectTaskRefMapper;
import com.vibe.resource.service.EngineerScheduleService;
import com.vibe.resource.service.TaskDispatchService;
import com.vibe.resource.vo.DispatchResultVO;
import com.vibe.resource.vo.EngineerRecommendationVO;
import com.vibe.resource.vo.EngineerSkillVO;
import com.vibe.resource.vo.EngineerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务派发服务实现
 *
 * <p>核心能力：</p>
 * <ul>
 *   <li>手动指派/批量派单/紧急调配：写 engineer_schedule（TASK 类型）+ 更新 project_task.assignee_id</li>
 *   <li>转派/退回：释放原排期 + 重新指派/回退任务状态</li>
 *   <li>智能推荐：技能匹配度(40%) + 区域就近(30%) + 当前负荷(30%) 加权评分</li>
 * </ul>
 *
 * <p>跨模块说明：通过 {@link ProjectTaskRefMapper} 直接操作 project_task 表，
 * 不依赖 module-project，保持模块分层独立。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDispatchServiceImpl implements TaskDispatchService {

    private final EngineerMapper engineerMapper;
    private final EngineerSkillMapper engineerSkillMapper;
    private final EngineerScheduleMapper scheduleMapper;
    private final EngineerScheduleService scheduleService;
    private final ProjectTaskRefMapper projectTaskRefMapper;

    /** 负荷评分：每多一个并发任务扣 25 分，扣到 0 为止 */
    private static final int WORKLOAD_PENALTY_PER_TASK = 25;
    /** 工作日每天标准工时（用于负荷折算，预留） */
    private static final int STANDARD_HOURS_PER_DAY = 8;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long dispatch(TaskDispatchDTO dto) {
        return doDispatch(dto, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DispatchResultVO batchDispatch(BatchDispatchDTO dto) {
        DispatchResultVO result = new DispatchResultVO();
        if (dto.getDispatches() == null) {
            return result;
        }
        for (TaskDispatchDTO item : dto.getDispatches()) {
            try {
                Long scheduleId = doDispatch(item, false);
                result.getSuccessIds().add(scheduleId);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (BusinessException e) {
                DispatchResultVO.FailItem fail = new DispatchResultVO.FailItem();
                fail.setTaskId(item.getTaskId());
                fail.setEngineerId(item.getEngineerId());
                fail.setReason(e.getMessage());
                result.getFailures().add(fail);
                result.setFailCount(result.getFailCount() + 1);
            } catch (Exception e) {
                DispatchResultVO.FailItem fail = new DispatchResultVO.FailItem();
                fail.setTaskId(item.getTaskId());
                fail.setEngineerId(item.getEngineerId());
                fail.setReason("系统异常：" + e.getMessage());
                result.getFailures().add(fail);
                result.setFailCount(result.getFailCount() + 1);
            }
        }
        return result;
    }

    @Override
    public List<EngineerRecommendationVO> recommend(TaskRecommendationQueryDTO query) {
        if (query.getStartTime() == null || query.getEndTime() == null
                || !query.getEndTime().isAfter(query.getStartTime())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "排期时间范围非法");
        }
        // 1. 按所需技能 + 区域查询可用工程师（ACTIVE）
        List<String> requiredSkills = query.getRequiredSkills();
        List<EngineerVO> candidates = engineerMapper.selectAvailableEngineers(
                requiredSkills, null, ResourceConstant.ENGINEER_STATUS_ACTIVE);
        if (CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyList();
        }

        // 2. 批量查询候选人技能
        List<Long> engineerIds = candidates.stream()
                .map(EngineerVO::getId).collect(Collectors.toList());
        List<EngineerSkillEntity> allSkills = engineerSkillMapper.selectByEngineerIds(engineerIds);

        // 3. 逐个评分
        List<EngineerRecommendationVO> recommendations = new ArrayList<>(candidates.size());
        String taskRegion = query.getRegion();
        Set<String> requiredSkillSet = requiredSkills == null
                ? Collections.emptySet() : new HashSet<>(requiredSkills);

        for (EngineerVO candidate : candidates) {
            EngineerRecommendationVO vo = new EngineerRecommendationVO();
            vo.setEngineerId(candidate.getId());
            vo.setEngineerName(candidate.getName());
            vo.setRegion(candidate.getRegion());
            vo.setStatus(candidate.getStatus());

            // 当前时段负荷
            int currentWorkload = scheduleService.countWorkload(
                    candidate.getId(), query.getStartTime(), query.getEndTime());
            vo.setCurrentWorkload(currentWorkload);

            // 冲突检测（TASK 类型）
            boolean hasConflict = currentWorkload > 0;
            vo.setHasConflict(hasConflict);

            // 该工程师的技能列表（从批量查询结果中过滤）
            List<EngineerSkillEntity> engineerSkills = allSkills.stream()
                    .filter(s -> s.getEngineerId().equals(candidate.getId()))
                    .collect(Collectors.toList());
            vo.setSkills(engineerSkills.stream().map(this::toSkillVO).collect(Collectors.toList()));

            // 技能匹配度评分
            BigDecimal skillScore = calcSkillScore(engineerSkills, requiredSkillSet);
            vo.setSkillScore(skillScore);

            // 区域就近评分
            BigDecimal regionScore = calcRegionScore(candidate.getRegion(), taskRegion);
            vo.setRegionScore(regionScore);

            // 负荷评分（负荷越低分越高）
            BigDecimal workloadScore = calcWorkloadScore(currentWorkload);
            vo.setWorkloadScore(workloadScore);

            // 综合评分 = skillScore * 0.4 + regionScore * 0.3 + workloadScore * 0.3
            BigDecimal score = skillScore.multiply(BigDecimal.valueOf(ResourceConstant.WEIGHT_SKILL))
                    .add(regionScore.multiply(BigDecimal.valueOf(ResourceConstant.WEIGHT_REGION)))
                    .add(workloadScore.multiply(BigDecimal.valueOf(ResourceConstant.WEIGHT_WORKLOAD)))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setScore(score);

            vo.setReason(buildReason(skillScore, regionScore, workloadScore, hasConflict));
            recommendations.add(vo);
        }

        // 4. 按综合评分降序，取 Top N
        int limit = query.getLimit() == null || query.getLimit() <= 0 ? 10 : query.getLimit();
        recommendations.sort(Comparator.comparing(EngineerRecommendationVO::getScore).reversed());
        if (recommendations.size() > limit) {
            return recommendations.subList(0, limit);
        }
        return recommendations;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reassign(TaskReassignDTO dto) {
        if (dto.getTaskId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "任务ID不能为空");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null
                || !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "排期时间范围非法");
        }
        // 释放原工程师排期
        scheduleMapper.deleteByTaskId(dto.getTaskId());

        // 新工程师冲突检测
        boolean ignoreConflict = Boolean.TRUE.equals(dto.getIgnoreConflict());
        if (!ignoreConflict) {
            var conflict = scheduleService.detectConflict(
                    dto.getNewEngineerId(), dto.getStartTime(), dto.getEndTime(), null);
            if (Boolean.TRUE.equals(conflict.getConflict())) {
                throw new BusinessException(ResultCode.SCHEDULE_CONFLICT,
                    "新工程师在该时段已有排期任务，存在冲突");
            }
        }
        // 校验新工程师
        EngineerEntity newEngineer = engineerMapper.selectById(dto.getNewEngineerId());
        if (newEngineer == null) {
            throw BusinessException.notFound("工程师");
        }

        // 写入新排期
        EngineerScheduleEntity schedule = new EngineerScheduleEntity();
        schedule.setEngineerId(dto.getNewEngineerId());
        schedule.setTaskId(dto.getTaskId());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setScheduleType(ResourceConstant.SCHEDULE_TYPE_TASK);
        schedule.setRemark(StringUtils.hasText(dto.getReason())
                ? "转派：" + dto.getReason() : "转派");
        scheduleMapper.insert(schedule);

        // 更新 project_task.assignee_id（关联 sys_user.id = engineer.user_id）
        projectTaskRefMapper.updateAssigneeAndStatus(
                dto.getTaskId(), newEngineer.getUserId(), ResourceConstant.TASK_ASSIGNED);
        log.info("[转派] taskId={} → 新工程师 engineerId={} (userId={})",
                dto.getTaskId(), dto.getNewEngineerId(), newEngineer.getUserId());
        return schedule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(TaskReturnDTO dto) {
        if (dto.getTaskId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "任务ID不能为空");
        }
        // 释放排期
        scheduleMapper.deleteByTaskId(dto.getTaskId());
        // 任务回到待分配
        projectTaskRefMapper.releaseAssignee(dto.getTaskId());
        log.info("[退回] taskId={} 已释放排期并回退为 PENDING，原因：{}",
                dto.getTaskId(), dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long urgentDispatch(TaskDispatchDTO dto) {
        return doDispatch(dto, true);
    }

    /* ============ 私有方法 ============ */

    /**
     * 派发核心逻辑：写排期 + 更新 project_task.assignee_id。
     *
     * @param dto            派发参数
     * @param forceIgnoreConflict 是否强制跳过冲突检测（紧急调配）
     */
    private Long doDispatch(TaskDispatchDTO dto, boolean forceIgnoreConflict) {
        if (dto.getTaskId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "任务ID不能为空");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null
                || !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "排期时间范围非法");
        }
        EngineerEntity engineer = engineerMapper.selectById(dto.getEngineerId());
        if (engineer == null) {
            throw BusinessException.notFound("工程师");
        }
        if (!ResourceConstant.ENGINEER_STATUS_ACTIVE.equals(engineer.getStatus())) {
            throw new BusinessException(ResultCode.STATE_NOT_ALLOWED,
                "工程师非在职状态，不可派单");
        }

        // 冲突检测（紧急调配或显式 ignoreConflict 跳过）
        boolean ignore = forceIgnoreConflict || Boolean.TRUE.equals(dto.getIgnoreConflict());
        if (!ignore) {
            var conflict = scheduleService.detectConflict(
                    dto.getEngineerId(), dto.getStartTime(), dto.getEndTime(), null);
            if (Boolean.TRUE.equals(conflict.getConflict())) {
                throw new BusinessException(ResultCode.SCHEDULE_CONFLICT,
                    "工程师在该时段已有排期任务，存在冲突");
            }
        }

        // 写入排期（TASK 类型）
        EngineerScheduleEntity schedule = new EngineerScheduleEntity();
        schedule.setEngineerId(dto.getEngineerId());
        schedule.setTaskId(dto.getTaskId());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setScheduleType(ResourceConstant.SCHEDULE_TYPE_TASK);
        schedule.setRemark(dto.getRemark());
        scheduleMapper.insert(schedule);

        // 更新 project_task.assignee_id（关联 sys_user.id = engineer.user_id）+ 状态 ASSIGNED
        projectTaskRefMapper.updateAssigneeAndStatus(
                dto.getTaskId(), engineer.getUserId(), ResourceConstant.TASK_ASSIGNED);
        log.info("[派单] taskId={} → engineerId={} (userId={}) scheduleId={} ignoreConflict={}",
                dto.getTaskId(), dto.getEngineerId(), engineer.getUserId(),
                schedule.getId(), ignore);
        return schedule.getId();
    }

    /**
     * 技能匹配度评分：匹配的所需技能数 / 所需技能总数 * 100。
     *
     * <p>若未指定所需技能（requiredSkills 为空），则所有工程师该项均给满分 100，
     * 由区域与负荷评分决定排序。</p>
     */
    private BigDecimal calcSkillScore(List<EngineerSkillEntity> engineerSkills,
                                      Set<String> requiredSkillSet) {
        if (requiredSkillSet.isEmpty()) {
            return BigDecimal.valueOf(100);
        }
        if (CollectionUtils.isEmpty(engineerSkills)) {
            return BigDecimal.ZERO;
        }
        Set<String> owned = engineerSkills.stream()
                .map(EngineerSkillEntity::getSkillTag)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        long matched = requiredSkillSet.stream().filter(owned::contains).count();
        return BigDecimal.valueOf(matched)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requiredSkillSet.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 区域就近评分：区域完全匹配 100，未指定任务区域 80（无法判断就近，给中等偏高），
     * 区域不匹配 40（保留可调度余地，不完全为 0）。
     */
    private BigDecimal calcRegionScore(String engineerRegion, String taskRegion) {
        if (!StringUtils.hasText(taskRegion)) {
            return BigDecimal.valueOf(80);
        }
        if (!StringUtils.hasText(engineerRegion)) {
            return BigDecimal.valueOf(40);
        }
        if (taskRegion.equalsIgnoreCase(engineerRegion)) {
            return BigDecimal.valueOf(100);
        }
        // 区域包含关系（如 taskRegion="北京-海淀"，engineerRegion="北京"）视为部分匹配
        if (engineerRegion.contains(taskRegion) || taskRegion.contains(engineerRegion)) {
            return BigDecimal.valueOf(70);
        }
        return BigDecimal.valueOf(40);
    }

    /**
     * 当前负荷评分：负荷越低分越高。
     * 评分 = max(0, 100 - 当前并发任务数 * 25)。
     */
    private BigDecimal calcWorkloadScore(int currentWorkload) {
        int score = 100 - currentWorkload * WORKLOAD_PENALTY_PER_TASK;
        if (score < 0) {
            score = 0;
        }
        return BigDecimal.valueOf(score);
    }

    private String buildReason(BigDecimal skillScore, BigDecimal regionScore,
                               BigDecimal workloadScore, boolean hasConflict) {
        StringBuilder sb = new StringBuilder();
        sb.append("技能").append(skillScore).append("/区域").append(regionScore)
                .append("/负荷").append(workloadScore);
        if (hasConflict) {
            sb.append("（存在时段冲突，需协调）");
        }
        return sb.toString();
    }

    private EngineerSkillVO toSkillVO(EngineerSkillEntity entity) {
        EngineerSkillVO vo = new EngineerSkillVO();
        vo.setId(entity.getId());
        vo.setEngineerId(entity.getEngineerId());
        vo.setSkillTag(entity.getSkillTag());
        vo.setLevel(entity.getLevel());
        return vo;
    }
}
