package com.vibe.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.agent.dto.OutsourceWorkloadQueryDTO;
import com.vibe.agent.entity.OutsourceWorkloadEntity;
import com.vibe.agent.vo.OutsourceWorkloadVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代理商工作量 Mapper
 *
 * <p>数据权限说明：工作量表无 agent_company_id 字段，数据权限通过 Service 层
 * 先校验父任务归属后查询，不在此处使用 @DataPermission。
 * 全局分页查询 {@link #selectWorkloadPage} 的 AGENT_ADMIN 过滤在 Service 层
 * 强制 {@code agentCompanyId = tenantId}，PM/SUPER_ADMIN/DIRECTOR 查看全部。</p>
 *
 * @author vibe
 */
@Mapper
public interface OutsourceWorkloadMapper extends BaseMapper<OutsourceWorkloadEntity> {

    /**
     * 按转包任务ID查询工作量记录列表。
     */
    List<OutsourceWorkloadVO> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 全局分页查询工作量（含项目名/代理商名/确认人名关联）。
     *
     * <p>JOIN outsource_task / project / agent_company / sys_user 获取关联名称。
     * AGENT_ADMIN 数据隔离在 Service 层通过强制 {@code query.agentCompanyId = tenantId} 实现。</p>
     *
     * @param page  分页参数
     * @param query 查询条件（agentCompanyId / projectId / status / beginTime / endTime）
     */
    IPage<OutsourceWorkloadVO> selectWorkloadPage(IPage<OutsourceWorkloadVO> page,
                                                  @Param("query") OutsourceWorkloadQueryDTO query);
}
