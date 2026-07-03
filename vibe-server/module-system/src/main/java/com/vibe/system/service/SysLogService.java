package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysLogQueryDTO;
import com.vibe.system.entity.SysLogEntity;
import com.vibe.system.entity.SysLoginLogEntity;
import com.vibe.system.vo.SysLogVO;
import com.vibe.system.vo.SysLoginLogVO;

/**
 * 系统日志服务（操作日志 + 登录日志）
 *
 * @author vibe
 */
public interface SysLogService {

    /* ============ 操作日志 ============ */

    PageResult<SysLogVO> pageOperationLog(SysLogQueryDTO query);

    /**
     * 异步记录操作日志（供 @OperationLog AOP 切面调用）
     */
    void asyncSave(SysLogEntity entity);

    /* ============ 登录日志 ============ */

    PageResult<SysLoginLogVO> pageLoginLog(Integer page, Integer size, String username,
                                           Integer status, String beginTime, String endTime);

    /**
     * 记录登录日志
     */
    void recordLoginLog(SysLoginLogEntity entity);
}
