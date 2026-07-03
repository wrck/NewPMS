package com.vibe.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.agent.entity.OutsourceWorkloadEntity;
import com.vibe.agent.vo.OutsourceWorkloadVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代理商工作量 Mapper
 *
 * <p>数据权限说明：工作量表无 agent_company_id 字段，数据权限通过 Service 层
 * 先校验父任务归属后查询，不在此处使用 @DataPermission。</p>
 *
 * @author vibe
 */
@Mapper
public interface OutsourceWorkloadMapper extends BaseMapper<OutsourceWorkloadEntity> {

    /**
     * 按转包任务ID查询工作量记录列表。
     */
    List<OutsourceWorkloadVO> selectByTaskId(@Param("taskId") Long taskId);
}
