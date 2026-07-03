package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysDictTypeDTO;
import com.vibe.system.dto.SysDictTypeQueryDTO;
import com.vibe.system.vo.SysDictTypeVO;

/**
 * 字典类型服务
 *
 * @author vibe
 */
public interface SysDictTypeService {

    PageResult<SysDictTypeVO> page(SysDictTypeQueryDTO query);

    Long create(SysDictTypeDTO dto);

    void update(SysDictTypeDTO dto);

    void delete(Long id);

    SysDictTypeVO getDetail(Long id);
}
