package com.vibe.integration.service;

import com.vibe.common.result.PageResult;
import com.vibe.integration.dto.IntegrationConfigQueryDTO;
import com.vibe.integration.dto.IntegrationConfigSaveDTO;
import com.vibe.integration.vo.IntegrationConfigVO;

import java.util.List;

/**
 * 集成配置 Service
 *
 * @author vibe
 */
public interface IntegrationConfigService {

    /**
     * 分页查询集成配置
     */
    PageResult<IntegrationConfigVO> page(IntegrationConfigQueryDTO query);

    /**
     * 详情
     */
    IntegrationConfigVO getDetail(Long id);

    /**
     * 按系统编码查询
     */
    IntegrationConfigVO getBySystemCode(String systemCode);

    /**
     * 查询所有启用的配置
     */
    List<IntegrationConfigVO> listEnabled();

    /**
     * 新增
     */
    Long save(IntegrationConfigSaveDTO dto);

    /**
     * 更新
     */
    void update(Long id, IntegrationConfigSaveDTO dto);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 启用/禁用
     */
    void toggleEnabled(Long id, Integer enabled);

    /**
     * 测试连接（更新最近调用状态）
     */
    boolean testConnection(Long id);
}
