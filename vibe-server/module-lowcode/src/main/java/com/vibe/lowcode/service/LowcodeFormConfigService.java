package com.vibe.lowcode.service;

import com.vibe.common.base.PageQuery;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.dto.LowcodeFormConfigDTO;
import com.vibe.lowcode.vo.LowcodeFormConfigVO;

/**
 * 低代码表单配置 Service
 *
 * @author vibe
 */
public interface LowcodeFormConfigService {

    /** 分页查询（keyword 模糊匹配 configCode / configName） */
    PageResult<LowcodeFormConfigVO> page(PageQuery query, String keyword);

    /** 获取详情 */
    LowcodeFormConfigVO getById(Long id);

    /** 创建配置 */
    Long create(LowcodeFormConfigDTO dto);

    /** 更新配置 */
    void update(Long id, LowcodeFormConfigDTO dto);

    /** 删除配置 */
    void delete(Long id);

    /** 复制配置（复制 schemaJson，新 configCode 加 _copy 后缀） */
    Long copy(Long id);

    /** 导出 JSON Schema */
    String exportJson(Long id);

    /** 导入 JSON Schema（带 JSON Schema Draft 7 校验） */
    Long importJson(LowcodeFormConfigDTO dto);

    /** 基于模板实例化（复制模板 schemaJson 到新配置，模板 usageCount +1） */
    Long instantiateFromTemplate(Long templateId, String configName);
}
