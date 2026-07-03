package com.vibe.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.annotation.DataPermission;
import com.vibe.delivery.dto.WorkOrderQueryDTO;
import com.vibe.delivery.entity.WorkOrderEntity;
import com.vibe.delivery.vo.WorkOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单 Mapper
 *
 * <p>数据权限：ENGINEER 只看 engineer_id = 当前 userId 的工单；
 * PM 看自己负责项目下的所有工单（pm_id 在 project 表，需通过 task 关联，此处用 project.pm_id）。</p>
 *
 * @author vibe
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrderEntity> {

    /**
     * 分页查询工单（关联 project_task / project / sys_user 工程师名）
     *
     * <p>数据权限通过 @DataPermission 拦截：ENGINEER 仅看 engineer_id = 自己 的工单。</p>
     */
    @DataPermission(table = "wo", engineerField = "engineer_id")
    IPage<WorkOrderVO> selectWorkOrderPage(IPage<WorkOrderVO> page, @Param("query") WorkOrderQueryDTO query);

    /**
     * 按 ID 查询工单详情（含任务名/项目名/工程师名）
     */
    WorkOrderVO selectVoById(@Param("id") Long id);

    /**
     * 按工程师 ID 查询工单列表（移动端"我的任务"用）
     *
     * @param engineerId 工程师用户ID
     * @param status     状态（可空）
     */
    @DataPermission(table = "wo", engineerField = "engineer_id")
    List<WorkOrderVO> selectByEngineer(@Param("engineerId") Long engineerId, @Param("status") String status);

    /**
     * 按任务 ID 查询工单列表
     */
    List<WorkOrderEntity> selectByTaskId(@Param("taskId") Long taskId);
}
