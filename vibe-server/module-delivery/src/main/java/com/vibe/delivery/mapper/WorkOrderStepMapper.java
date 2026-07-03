package com.vibe.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.delivery.entity.WorkOrderStepEntity;
import com.vibe.delivery.vo.WorkOrderStepVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单施工步骤 Mapper
 *
 * @author vibe
 */
@Mapper
public interface WorkOrderStepMapper extends BaseMapper<WorkOrderStepEntity> {

    /**
     * 按工单 ID 查询步骤列表（按 step_no 排序）
     */
    List<WorkOrderStepVO> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    /**
     * 按工单 ID 查询所有步骤实体（service 层统计进度用）
     */
    List<WorkOrderStepEntity> selectEntitiesByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
