package com.vibe.delivery.mapper;

import com.vibe.delivery.bo.ProjectTaskLookup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目任务轻量查询 Mapper
 *
 * <p>module-delivery 不直接依赖 module-project（避免循环依赖与未实现模块耦合），
 * 通过本 Mapper 直接查询 project_task 表获取工单创建/确认所需的最小字段集合。</p>
 *
 * <p>仅 SELECT，不修改 project_task 状态；状态推进由 {@code updateTaskStatus} 完成，
 * 待 module-project 落地后可替换为事件驱动方式。</p>
 *
 * @author vibe
 */
@Mapper
public interface ProjectTaskLookupMapper {

    /**
     * 按 ID 查询任务轻量信息
     */
    ProjectTaskLookup selectById(@Param("id") Long id);

    /**
     * 更新任务状态与实际结束日期（PM 确认工单完成时调用）
     *
     * <p>本方法直接 UPDATE project_task，受 MyBatis-Plus 逻辑删除全局配置影响
     * （未在 project_task 上加 @TableLogic，故需显式带 deleted=0 条件）。</p>
     *
     * @param id         任务ID
     * @param status     目标状态
     * @param actualEnd  实际结束日期（可空）
     * @param operatorId 操作人ID
     * @return 影响行数
     */
    int updateTaskStatus(@Param("id") Long id,
                         @Param("status") String status,
                         @Param("actualEnd") java.time.LocalDate actualEnd,
                         @Param("operatorId") Long operatorId);

    /**
     * 查询项目下未确认完成的任务数（用于自动推进项目进度校验）
     */
    int countUnconfirmedByProject(@Param("projectId") Long projectId);

    /**
     * 更新项目进度百分比
     */
    int updateProjectProgress(@Param("projectId") Long projectId,
                              @Param("progressPct") Integer progressPct,
                              @Param("operatorId") Long operatorId);

    /**
     * 查询项目下任务总数
     */
    int countTotalByProject(@Param("projectId") Long projectId);

    /**
     * 查询项目下已完成（COMPLETED/CONFIRMED）任务数
     */
    int countCompletedByProject(@Param("projectId") Long projectId);

    /**
     * 查询项目下所有任务ID（用于进度推进）
     */
    List<Long> selectTaskIdsByProject(@Param("projectId") Long projectId);
}
