package com.vibe.resource.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.resource.constant.ResourceConstant;
import com.vibe.resource.dto.EngineerLeaveDTO;
import com.vibe.resource.dto.EngineerLeaveQueryDTO;
import com.vibe.resource.dto.EngineerScheduleDTO;
import com.vibe.resource.dto.EngineerScheduleQueryDTO;
import com.vibe.resource.entity.EngineerEntity;
import com.vibe.resource.entity.EngineerLeaveEntity;
import com.vibe.resource.entity.EngineerScheduleEntity;
import com.vibe.resource.mapper.EngineerLeaveMapper;
import com.vibe.resource.mapper.EngineerMapper;
import com.vibe.resource.mapper.EngineerScheduleMapper;
import com.vibe.resource.service.EngineerScheduleService;
import com.vibe.resource.vo.ConflictDetectVO;
import com.vibe.resource.vo.EngineerLeaveVO;
import com.vibe.resource.vo.EngineerScheduleVO;
import com.vibe.resource.vo.WorkloadHeatmapVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 工程师排期服务实现
 *
 * <p>核心能力：</p>
 * <ul>
 *   <li>排期 CRUD + 日历视图</li>
 *   <li>冲突检测：基于 engineer_schedule 表时间区间重叠判断
 *       （existing.start_time &lt; new.end_time AND existing.end_time &gt; new.start_time）</li>
 *   <li>负荷热力图</li>
 *   <li>请假管理：审批通过后自动写入 LEAVE 类型排期，标记不可分配时段</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EngineerScheduleServiceImpl implements EngineerScheduleService {

    private final EngineerScheduleMapper scheduleMapper;
    private final EngineerMapper engineerMapper;
    private final EngineerLeaveMapper leaveMapper;

    /** 负荷等级阈值：当日任务数 */
    private static final int LOAD_THRESHOLD_LOW = 1;
    private static final int LOAD_THRESHOLD_MEDIUM = 2;
    private static final int LOAD_THRESHOLD_HIGH = 3;
    /** 负荷等级：超过 HIGH 阈值判定为 OVERLOAD */
    private static final String LOAD_LEVEL_LOW = "LOW";
    private static final String LOAD_LEVEL_MEDIUM = "MEDIUM";
    private static final String LOAD_LEVEL_HIGH = "HIGH";
    private static final String LOAD_LEVEL_OVERLOAD = "OVERLOAD";

    @Override
    public PageResult<EngineerScheduleVO> page(EngineerScheduleQueryDTO query) {
        IPage<EngineerScheduleVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<EngineerScheduleVO> result = scheduleMapper.selectSchedulePage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public List<EngineerScheduleVO> calendar(Long engineerId, Long taskId,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             String scheduleType) {
        return scheduleMapper.selectCalendar(engineerId, taskId, startTime, endTime, scheduleType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(EngineerScheduleDTO dto) {
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        // TASK 类型校验工程师存在
        EngineerEntity engineer = engineerMapper.selectById(dto.getEngineerId());
        if (engineer == null) {
            throw BusinessException.notFound("工程师");
        }
        // 冲突检测（紧急调配 ignoreConflict=true 时跳过）
        boolean ignoreConflict = Boolean.TRUE.equals(dto.getIgnoreConflict());
        if (!ignoreConflict) {
            ConflictDetectVO conflict = detectConflict(dto.getEngineerId(),
                    dto.getStartTime(), dto.getEndTime(), null);
            if (Boolean.TRUE.equals(conflict.getConflict())) {
                throw new BusinessException(ResultCode.SCHEDULE_CONFLICT,
                    "工程师在该时段已有排期任务，存在冲突");
            }
        }
        EngineerScheduleEntity entity = new EngineerScheduleEntity();
        entity.setEngineerId(dto.getEngineerId());
        entity.setTaskId(dto.getTaskId());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setScheduleType(StringUtils.hasText(dto.getScheduleType())
                ? dto.getScheduleType() : ResourceConstant.SCHEDULE_TYPE_TASK);
        entity.setRemark(dto.getRemark());
        scheduleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSchedule(EngineerScheduleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "排期ID不能为空");
        }
        EngineerScheduleEntity exist = scheduleMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.notFound("排期");
        }
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        // 编辑时排除自身做冲突检测
        boolean ignoreConflict = Boolean.TRUE.equals(dto.getIgnoreConflict());
        if (!ignoreConflict) {
            ConflictDetectVO conflict = detectConflict(dto.getEngineerId(),
                    dto.getStartTime(), dto.getEndTime(), dto.getId());
            if (Boolean.TRUE.equals(conflict.getConflict())) {
                throw new BusinessException(ResultCode.SCHEDULE_CONFLICT,
                    "工程师在该时段已有排期任务，存在冲突");
            }
        }
        exist.setEngineerId(dto.getEngineerId());
        exist.setTaskId(dto.getTaskId());
        exist.setStartTime(dto.getStartTime());
        exist.setEndTime(dto.getEndTime());
        if (StringUtils.hasText(dto.getScheduleType())) {
            exist.setScheduleType(dto.getScheduleType());
        }
        exist.setRemark(dto.getRemark());
        scheduleMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "排期ID不能为空");
        }
        scheduleMapper.deleteById(id);
    }

    /**
     * 冲突检测：查询 engineer_schedule 中该工程师该时间段是否有重叠的 TASK 类型排期记录。
     *
     * <p>重叠判定：existing.start_time &lt; new.end_time AND existing.end_time &gt; new.start_time</p>
     * <p>默认仅检测 TASK 类型（LEAVE/TRAINING/MEETING 不视作分配冲突）</p>
     */
    @Override
    public ConflictDetectVO detectConflict(Long engineerId,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           Long excludeId) {
        ConflictDetectVO vo = new ConflictDetectVO();
        vo.setEngineerId(engineerId);
        vo.setStartTime(startTime);
        vo.setEndTime(endTime);
        if (engineerId == null) {
            vo.setConflict(false);
            vo.setConflictSchedules(Collections.emptyList());
            return vo;
        }
        // 工程师姓名
        EngineerEntity engineer = engineerMapper.selectById(engineerId);
        if (engineer != null) {
            vo.setEngineerName(engineer.getName());
        }
        // 仅检测 TASK 类型冲突
        List<EngineerScheduleEntity> conflicts = scheduleMapper.selectConflicts(
                engineerId, startTime, endTime, excludeId,
                Collections.singletonList(ResourceConstant.SCHEDULE_TYPE_TASK));
        List<EngineerScheduleVO> conflictVos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(conflicts)) {
            for (EngineerScheduleEntity e : conflicts) {
                EngineerScheduleVO v = new EngineerScheduleVO();
                v.setId(e.getId());
                v.setEngineerId(e.getEngineerId());
                v.setTaskId(e.getTaskId());
                v.setStartTime(e.getStartTime());
                v.setEndTime(e.getEndTime());
                v.setScheduleType(e.getScheduleType());
                v.setRemark(e.getRemark());
                if (engineer != null) {
                    v.setEngineerName(engineer.getName());
                }
                conflictVos.add(v);
            }
        }
        vo.setConflict(!conflictVos.isEmpty());
        vo.setConflictSchedules(conflictVos);
        return vo;
    }

    @Override
    public List<WorkloadHeatmapVO> workloadHeatmap(LocalDateTime startTime, LocalDateTime endTime,
                                                    String region, List<Long> engineerIds) {
        if (startTime == null || endTime == null) {
            return Collections.emptyList();
        }
        List<WorkloadHeatmapVO> list = engineerMapper.selectWorkloadHeatmap(
                engineerIds, startTime, endTime, region);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 填充负荷等级
        for (WorkloadHeatmapVO vo : list) {
            vo.setLoadLevel(calcLoadLevel(vo.getTaskCount() == null ? 0 : vo.getTaskCount()));
        }
        return list;
    }

    @Override
    public Integer countWorkload(Long engineerId, LocalDateTime startTime, LocalDateTime endTime) {
        if (engineerId == null || startTime == null || endTime == null) {
            return 0;
        }
        List<EngineerScheduleEntity> conflicts = scheduleMapper.selectConflicts(
                engineerId, startTime, endTime, null,
                Collections.singletonList(ResourceConstant.SCHEDULE_TYPE_TASK));
        return conflicts == null ? 0 : conflicts.size();
    }

    /* ============ 请假/培训时间块管理 ============ */

    @Override
    public PageResult<EngineerLeaveVO> leavePage(EngineerLeaveQueryDTO query) {
        IPage<EngineerLeaveVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<EngineerLeaveVO> result = leaveMapper.selectLeavePage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLeave(EngineerLeaveDTO dto) {
        if (dto.getStartDate() == null || dto.getEndDate() == null
                || dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "请假日期范围非法");
        }
        EngineerLeaveEntity entity = new EngineerLeaveEntity();
        entity.setEngineerId(dto.getEngineerId());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setLeaveType(StringUtils.hasText(dto.getLeaveType())
                ? dto.getLeaveType() : ResourceConstant.LEAVE_TYPE_OTHER);
        entity.setReason(dto.getReason());
        entity.setStatus(StringUtils.hasText(dto.getStatus())
                ? dto.getStatus() : ResourceConstant.APPROVAL_PENDING);
        leaveMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLeave(EngineerLeaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请假ID不能为空");
        }
        EngineerLeaveEntity exist = leaveMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.notFound("请假记录");
        }
        if (ResourceConstant.APPROVAL_APPROVED.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("已批准的请假不可编辑");
        }
        exist.setEngineerId(dto.getEngineerId());
        exist.setStartDate(dto.getStartDate());
        exist.setEndDate(dto.getEndDate());
        if (StringUtils.hasText(dto.getLeaveType())) {
            exist.setLeaveType(dto.getLeaveType());
        }
        exist.setReason(dto.getReason());
        if (StringUtils.hasText(dto.getStatus())) {
            exist.setStatus(dto.getStatus());
        }
        leaveMapper.updateById(exist);
    }

    /**
     * 审批请假：APPROVED 时同步写入 engineer_schedule 的 LEAVE 时间块，标记不可分配时段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveLeave(Long id, String decision) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请假ID不能为空");
        }
        if (!ResourceConstant.APPROVAL_APPROVED.equals(decision)
                && !ResourceConstant.APPROVAL_REJECTED.equals(decision)) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                "审批结果非法，仅支持 APPROVED / REJECTED");
        }
        EngineerLeaveEntity exist = leaveMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.notFound("请假记录");
        }
        if (!ResourceConstant.APPROVAL_PENDING.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("当前请假状态不允许审批");
        }
        exist.setStatus(decision);
        leaveMapper.updateById(exist);

        // 批准后写入 LEAVE 类型排期，标记不可分配时段
        if (ResourceConstant.APPROVAL_APPROVED.equals(decision)) {
            EngineerScheduleEntity schedule = new EngineerScheduleEntity();
            schedule.setEngineerId(exist.getEngineerId());
            schedule.setStartTime(exist.getStartDate().atStartOfDay());
            // 结束日期取末日 23:59:59
            schedule.setEndTime(exist.getEndDate().atTime(23, 59, 59));
            schedule.setScheduleType(ResourceConstant.SCHEDULE_TYPE_LEAVE);
            schedule.setRemark("请假自动排期：" + exist.getReason());
            scheduleMapper.insert(schedule);
            log.info("[请假审批] 请假ID={} 已批准，自动写入 LEAVE 排期 engineerId={}",
                    id, exist.getEngineerId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLeave(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请假ID不能为空");
        }
        leaveMapper.deleteById(id);
    }

    /* ============ 私有方法 ============ */

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "开始/结束时间不能为空");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "结束时间必须晚于开始时间");
        }
    }

    private String calcLoadLevel(int taskCount) {
        if (taskCount <= 0) {
            return LOAD_LEVEL_LOW;
        }
        if (taskCount <= LOAD_THRESHOLD_LOW) {
            return LOAD_LEVEL_LOW;
        }
        if (taskCount <= LOAD_THRESHOLD_MEDIUM) {
            return LOAD_LEVEL_MEDIUM;
        }
        if (taskCount <= LOAD_THRESHOLD_HIGH) {
            return LOAD_LEVEL_HIGH;
        }
        return LOAD_LEVEL_OVERLOAD;
    }
}
