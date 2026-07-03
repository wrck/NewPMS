package com.vibe.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.delivery.entity.WorkOrderPhotoEntity;
import com.vibe.delivery.vo.WorkOrderPhotoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单施工照片 Mapper
 *
 * @author vibe
 */
@Mapper
public interface WorkOrderPhotoMapper extends BaseMapper<WorkOrderPhotoEntity> {

    /**
     * 按工单 ID 查询照片列表
     */
    List<WorkOrderPhotoVO> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    /**
     * 按步骤 ID 查询照片列表
     */
    List<WorkOrderPhotoVO> selectByStepId(@Param("stepId") Long stepId);

    /**
     * 按工单 ID 统计照片数量
     */
    int countByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
