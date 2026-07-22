package com.vibe.agent.service;

import com.vibe.agent.dto.OutsourceWorkloadDTO;
import com.vibe.agent.dto.OutsourceWorkloadQueryDTO;
import com.vibe.agent.vo.OutsourceWorkloadVO;
import com.vibe.common.result.PageResult;

import java.util.List;

/**
 * 代理商工作量服务
 *
 * <p>提供工作量提交（人天/站点数/设备台数）与 PM 确认。</p>
 *
 * @author vibe
 */
public interface OutsourceWorkloadService {

    /**
     * 代理商提交工作量。
     *
     * @return 工作量记录ID
     */
    Long submit(OutsourceWorkloadDTO dto);

    /**
     * PM 确认工作量。
     *
     * @param id 工作量记录ID
     */
    void confirm(Long id);

    /**
     * PM 驳回工作量。
     *
     * @param id     工作量记录ID
     * @param remark 驳回原因
     */
    void reject(Long id, String remark);

    /**
     * 按任务ID查询工作量记录列表。
     */
    List<OutsourceWorkloadVO> listByTaskId(Long taskId);

    /**
     * 全局分页查询工作量（含项目名/代理商名/确认人名）。
     *
     * <p>AGENT_ADMIN 仅查看本公司工作量（强制 agentCompanyId = tenantId），
     * PM/SUPER_ADMIN/DIRECTOR 查看全部。</p>
     */
    PageResult<OutsourceWorkloadVO> page(OutsourceWorkloadQueryDTO query);
}
