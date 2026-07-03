package com.vibe.resource.service;

import com.vibe.resource.dto.BatchDispatchDTO;
import com.vibe.resource.dto.TaskDispatchDTO;
import com.vibe.resource.dto.TaskRecommendationQueryDTO;
import com.vibe.resource.dto.TaskReassignDTO;
import com.vibe.resource.dto.TaskReturnDTO;
import com.vibe.resource.vo.DispatchResultVO;
import com.vibe.resource.vo.EngineerRecommendationVO;

import java.util.List;

/**
 * 任务派发服务
 *
 * @author vibe
 */
public interface TaskDispatchService {

    /**
     * 手动指派：指定工程师，设置 engineer_schedule，并更新 project_task.assignee_id
     */
    Long dispatch(TaskDispatchDTO dto);

    /**
     * 批量派单：返回成功/失败明细
     */
    DispatchResultVO batchDispatch(BatchDispatchDTO dto);

    /**
     * 智能推荐：基于技能匹配/区域就近/当前负荷返回推荐工程师列表
     */
    List<EngineerRecommendationVO> recommend(TaskRecommendationQueryDTO query);

    /**
     * 转派：释放原工程师排期，分配新工程师
     */
    Long reassign(TaskReassignDTO dto);

    /**
     * 退回：释放排期，任务回到待分配（PENDING）
     */
    void returnTask(TaskReturnDTO dto);

    /**
     * 紧急调配：跳过冲突检测强制指派（管理层干预）
     */
    Long urgentDispatch(TaskDispatchDTO dto);
}
