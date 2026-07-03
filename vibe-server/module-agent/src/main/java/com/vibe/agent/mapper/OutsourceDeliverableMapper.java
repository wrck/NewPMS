package com.vibe.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.agent.entity.OutsourceDeliverableEntity;
import com.vibe.agent.vo.OutsourceDeliverableVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 代理商交付物 Mapper
 *
 * <p>数据权限说明：交付物表无 agent_company_id 字段，数据权限通过 Service 层
 * 先校验父任务归属后查询，不在此处使用 @DataPermission。</p>
 *
 * @author vibe
 */
@Mapper
public interface OutsourceDeliverableMapper extends BaseMapper<OutsourceDeliverableEntity> {

    /**
     * 按转包任务ID查询交付物列表。
     */
    List<OutsourceDeliverableVO> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 按转包任务ID统计各类型交付物数量。
     *
     * @param taskId 转包任务ID
     * @return List of {deliverableType=xxx, cnt=N}
     */
    List<Map<String, Object>> countByTaskId(@Param("taskId") Long taskId);
}
