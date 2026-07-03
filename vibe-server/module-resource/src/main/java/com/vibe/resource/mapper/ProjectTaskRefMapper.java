package com.vibe.resource.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 跨模块引用 Mapper：直接操作 project_task 表的指派/状态字段
 *
 * <p>设计说明：资源调度模块在派发/转派/退回时需更新 project_task 表的 assignee_id 与 status。
 * 由于 module-resource 不依赖 module-project（保持模块分层独立），且 ProjectTaskMapper 尚未实现，
 * 此处通过自定义 SQL 直接操作 project_task 表，仅暴露必要的更新方法，避免跨模块 Mapper 注入。
 * 待 module-project 完整实现后，可改为事件驱动（发布 TaskDispatchedEvent 由 module-project 监听）。</p>
 *
 * <p>该 Mapper 不继承 BaseMapper，仅做最小化 SQL 操作；表 project_task 已含 version 乐观锁，
 * 此处通过 SET version = version + 1 同时推进版本号以保持乐观锁语义。</p>
 *
 * @author vibe
 */
@Mapper
public interface ProjectTaskRefMapper {

    /**
     * 更新任务执行人与状态（派发/转派时调用）
     *
     * @param taskId     项目任务ID
     * @param assigneeId 执行人ID（关联 sys_user.id；与 engineer.user_id 一致）
     * @param status     目标状态 ASSIGNED/IN_PROGRESS/COMPLETED/CONFIRMED/PENDING
     * @return 影响行数
     */
    int updateAssigneeAndStatus(@Param("taskId") Long taskId,
                                @Param("assigneeId") Long assigneeId,
                                @Param("status") String status);

    /**
     * 释放任务执行人（退回时调用，状态回到 PENDING）
     *
     * @param taskId 项目任务ID
     * @return 影响行数
     */
    int releaseAssignee(@Param("taskId") Long taskId);

    /**
     * 查询任务关联的项目ID（用于派发时回填 schedule 的项目信息）
     *
     * @param taskId 项目任务ID
     * @return 项目ID
     */
    Long selectProjectIdByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询任务的计划开始/结束日期（用于智能推荐与默认排期）。
     * 返回 List，0 位为 planned_start，1 位为 planned_end（任一可能为 null）。
     *
     * @param taskId 项目任务ID
     * @return 计划日期列表（长度 2，顺序 planned_start / planned_end）
     */
    List<LocalDate> selectTaskPlanRange(@Param("taskId") Long taskId);
}
