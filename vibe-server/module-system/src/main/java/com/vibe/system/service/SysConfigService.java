package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysConfigDTO;
import com.vibe.system.dto.SysConfigQueryDTO;
import com.vibe.system.vo.SysConfigVO;

/**
 * 系统配置服务
 *
 * @author vibe
 */
public interface SysConfigService {

    PageResult<SysConfigVO> page(SysConfigQueryDTO query);

    Long create(SysConfigDTO dto);

    void update(SysConfigDTO dto);

    void delete(Long id);

    SysConfigVO getDetail(Long id);

    /**
     * 按 configKey 查询配置值（带 Redis 缓存）。
     * 缓存 Key：vibe:sys:config:{configKey}
     */
    String getConfigValue(String configKey);

    /**
     * 清除指定 configKey 的缓存
     */
    void clearCache(String configKey);
}
