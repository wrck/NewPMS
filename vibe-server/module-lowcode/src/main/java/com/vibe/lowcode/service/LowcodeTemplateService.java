package com.vibe.lowcode.service;

import com.vibe.common.base.PageQuery;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.dto.LowcodeTemplateDTO;
import com.vibe.lowcode.vo.LowcodeTemplateVO;

/**
 * 低代码模板 Service
 *
 * @author vibe
 */
public interface LowcodeTemplateService {

    /** 分页查询（keyword 模糊匹配 templateCode / templateName） */
    PageResult<LowcodeTemplateVO> page(PageQuery query, String keyword);

    /** 获取详情 */
    LowcodeTemplateVO getById(Long id);

    /** 创建模板 */
    Long create(LowcodeTemplateDTO dto);

    /** 更新模板 */
    void update(Long id, LowcodeTemplateDTO dto);

    /** 删除模板 */
    void delete(Long id);

    /** 复制模板（复制 schemaJson，新 templateCode 加 _copy 后缀） */
    Long copy(Long id);

    /** 导出 JSON Schema */
    String exportJson(Long id);

    /** 导入 JSON Schema（带 JSON Schema Draft 7 校验） */
    Long importJson(LowcodeTemplateDTO dto);

    /** 基于模板实例化占位（模板本身不支持实例化，由各配置 Service 调用）；
     *  此方法仅校验模板存在并返回模板 ID，实际实例化由配置 Service 完成 */
    Long instantiateFromTemplate(Long templateId, String configName);
}
