package com.vibe.acceptance.service;

import com.vibe.acceptance.dto.AcceptanceDocQueryDTO;
import com.vibe.acceptance.dto.AcceptanceDocSaveDTO;
import com.vibe.acceptance.vo.AcceptanceDocVO;
import com.vibe.common.result.PageResult;

/**
 * 竣工文档 Service
 *
 * @author vibe
 */
public interface AcceptanceDocService {

    /**
     * 分页查询竣工文档
     */
    PageResult<AcceptanceDocVO> page(AcceptanceDocQueryDTO query);

    /**
     * 获取竣工文档详情
     */
    AcceptanceDocVO getDetail(Long id);

    /**
     * 上传/创建竣工文档
     */
    Long save(AcceptanceDocSaveDTO dto);

    /**
     * 更新竣工文档
     */
    void update(Long id, AcceptanceDocSaveDTO dto);

    /**
     * 删除竣工文档
     */
    void delete(Long id);
}
