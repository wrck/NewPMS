package com.vibe.project.service;

import com.vibe.common.result.PageResult;
import com.vibe.project.dto.BatchTaskDispatchDTO;
import com.vibe.project.dto.ProjectTaskDTO;
import com.vibe.project.dto.ProjectTaskQueryDTO;
import com.vibe.project.dto.TaskDispatchDTO;
import com.vibe.project.dto.TaskProgressDTO;
import com.vibe.project.dto.TaskReturnDTO;
import com.vibe.project.dto.TaskTransferDTO;
import com.vibe.project.vo.ProjectGanttVO;
import com.vibe.project.vo.ProjectTaskVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目任务服务
 *
 * <p>核心业务：</p>
 * <ul>
 *   <li>任务分解（父子任务）、依赖关系校验</li>
 *   <li>任务派发（SELF→assignee_id / AGENT→设置代理商字段）</li>
 *   <li>批量派单、转派、退回</li>
 *   <li>进度更新、进度自动同步（父子任务）、进度预警</li>
 *   <li>甘特图拖拽排期、依赖冲突检测</li>
 * </ul>
 *
 * @author vibe
 */
public interface ProjectTaskService {

    /**
     * 分页查询任务（含数据权限过滤）
     */
    PageResult<ProjectTaskVO> page(ProjectTaskQueryDTO query);

    /**
     * 查询项目下的全部任务
     */
    List<ProjectTaskVO> listByProjectId(Long projectId);

    /**
     * 任务详情
     */
    ProjectTaskVO getDetail(Long id);

    /**
     * 新增任务（支持父子任务分解）
     */
    Long create(ProjectTaskDTO dto);

    /**
     * 编辑任务
     */
    void update(ProjectTaskDTO dto);

    /**
     * 删除任务（存在子任务时不允许删除）
     */
    void delete(Long id);

    /**
     * 任务派发（SELF→assignee_id / AGENT→agent_company_id）
     */
    void dispatch(Long taskId, TaskDispatchDTO dto);

    /**
     * 批量派单
     *
     * @return 成功派发任务数
     */
    int batchDispatch(BatchTaskDispatchDTO dto);

    /**
     * 转派（更换执行人或代理商）
     */
    void transfer(Long taskId, TaskTransferDTO dto);

    /**
     * 退回（ASSIGNED/IN_PROGRESS → PENDING，清空执行人）
     */
    void returnTask(Long taskId, TaskReturnDTO dto);

    /**
     * 进度更新（状态流转，含乐观锁校验）
     */
    void updateProgress(Long taskId, TaskProgressDTO dto);

    /**
     * 进度自动同步：父任务进度 = 子任务平均进度；
     * 项目进度 = 全部任务完成率。由状态变更触发。
     */
    void syncProgress(Long projectId);

    /**
     * 进度预警：返回已超期但未完成的任务列表
     */
    List<ProjectTaskVO> listOverdueTasks();

    /**
     * 甘特图拖拽排期（更新计划开始/结束，含依赖冲突检测）
     */
    void reschedule(Long taskId, LocalDate newStart, LocalDate newEnd);
}

