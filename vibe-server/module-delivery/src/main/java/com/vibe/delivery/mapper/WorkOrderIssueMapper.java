package com.vibe.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.delivery.entity.WorkOrderIssueEntity;
import com.vibe.delivery.vo.WorkOrderIssueVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单异常问题 Mapper
 *
 * @author vibe
 */
@Mapper
public interface WorkOrderIssueMapper extends BaseMapper<WorkOrderIssueEntity> {

    /**
     * 按工单 ID 查询异常问题列表
     */
    List<WorkOrderIssueVO> selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    /**
     * 按 ID 查询异常问题详情
     */
    WorkOrderIssueVO selectVoById(@Param("id") Long id);
}
