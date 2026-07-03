package com.vibe.resource.service;

import com.vibe.common.result.PageResult;
import com.vibe.resource.dto.EngineerLeaveDTO;
import com.vibe.resource.dto.EngineerLeaveQueryDTO;
import com.vibe.resource.dto.EngineerScheduleDTO;
import com.vibe.resource.dto.EngineerScheduleQueryDTO;
import com.vibe.resource.vo.ConflictDetectVO;
import com.vibe.resource.vo.EngineerLeaveVO;
import com.vibe.resource.vo.EngineerScheduleVO;
import com.vibe.resource.vo.WorkloadHeatmapVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工程师排期服务
 *
 * @author vibe
 */
public interface EngineerScheduleService {

    /**
     * 分页查询排期
     */
    PageResult<EngineerScheduleVO> page(EngineerScheduleQueryDTO query);

    /**
     * 日历视图查询（按工程师 + 时间范围）
     */
    List<EngineerScheduleVO> calendar(Long engineerId, Long taskId,
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      String scheduleType);

    /**
     * 新增排期（含冲突检测，紧急调配可置 ignoreConflict=true 跳过）
     */
    Long createSchedule(EngineerScheduleDTO dto);

    /**
     * 编辑排期
     */
    void updateSchedule(EngineerScheduleDTO dto);

    /**
     * 删除排期
     */
    void deleteSchedule(Long id);

    /**
     * 冲突检测：返回是否冲突及冲突排期列表
     */
    ConflictDetectVO detectConflict(Long engineerId,
                                    LocalDateTime startTime, LocalDateTime endTime,
                                    Long excludeId);

    /**
     * 负荷热力图数据（各工程师某时段任务数 + 负荷等级）
     */
    List<WorkloadHeatmapVO> workloadHeatmap(LocalDateTime startTime, LocalDateTime endTime,
                                            String region, List<Long> engineerIds);

    /**
     * 工程师在某时段内的任务数（负荷计算用）
     */
    Integer countWorkload(Long engineerId, LocalDateTime startTime, LocalDateTime endTime);

    /* ============ 请假/培训时间块管理 ============ */

    /**
     * 分页查询请假记录
     */
    PageResult<EngineerLeaveVO> leavePage(EngineerLeaveQueryDTO query);

    /**
     * 新增请假（请假批准后自动写入 LEAVE 类型排期，标记不可分配时段）
     */
    Long createLeave(EngineerLeaveDTO dto);

    /**
     * 编辑请假
     */
    void updateLeave(EngineerLeaveDTO dto);

    /**
     * 审批请假（APPROVED 时同步写入排期 LEAVE 块）
     */
    void approveLeave(Long id, String decision);

    /**
     * 删除请假
     */
    void deleteLeave(Long id);
}
