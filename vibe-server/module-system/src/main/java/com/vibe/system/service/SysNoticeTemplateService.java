package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysNoticeTemplateDTO;
import com.vibe.system.dto.SysNoticeTemplateQueryDTO;
import com.vibe.system.entity.SysNoticeTemplateEntity;
import com.vibe.system.vo.SysNoticeTemplateVO;

/**
 * 通知模板服务
 *
 * @author vibe
 */
public interface SysNoticeTemplateService {

    PageResult<SysNoticeTemplateVO> page(SysNoticeTemplateQueryDTO query);

    Long create(SysNoticeTemplateDTO dto);

    void update(SysNoticeTemplateDTO dto);

    void delete(Long id);

    SysNoticeTemplateVO getDetail(Long id);

    /**
     * 按模板编码查询模板
     */
    SysNoticeTemplateEntity getByTemplateCode(String templateCode);
}
