package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysPositionDTO;
import com.vibe.system.vo.SysPositionVO;

/**
 * 岗位服务
 *
 * @author vibe
 */
public interface SysPositionService {

    PageResult<SysPositionVO> page(Integer page, Integer size, String keyword, Long orgId);

    Long create(SysPositionDTO dto);

    void update(SysPositionDTO dto);

    void delete(Long id);

    SysPositionVO getDetail(Long id);
}
