package com.vibe.integration.service;

import com.vibe.common.result.PageResult;
import com.vibe.integration.dto.IntegrationCallLogQueryDTO;
import com.vibe.integration.vo.IntegrationCallLogVO;

/**
 * 集成调用日志 Service
 *
 * @author vibe
 */
public interface IntegrationCallLogService {

    /**
     * 分页查询调用日志
     */
    PageResult<IntegrationCallLogVO> page(IntegrationCallLogQueryDTO query);

    /**
     * 详情
     */
    IntegrationCallLogVO getDetail(Long id);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 清空所有日志
     */
    void clearAll();
}
