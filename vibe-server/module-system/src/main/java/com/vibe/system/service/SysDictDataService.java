package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysDictDataDTO;
import com.vibe.system.dto.SysDictDataQueryDTO;
import com.vibe.system.vo.SysDictDataVO;

import java.util.List;

/**
 * 字典数据服务
 *
 * @author vibe
 */
public interface SysDictDataService {

    PageResult<SysDictDataVO> page(SysDictDataQueryDTO query);

    Long create(SysDictDataDTO dto);

    void update(SysDictDataDTO dto);

    void delete(Long id);

    SysDictDataVO getDetail(Long id);

    /**
     * 按 dictType 查询启用的字典数据（带 Redis 缓存）。
     * 缓存 Key：vibe:dict:{dictType}
     */
    List<SysDictDataVO> listByDictType(String dictType);

    /**
     * 清除指定 dictType 的缓存
     */
    void clearCache(String dictType);
}
