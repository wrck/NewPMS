package com.vibe.acceptance.service;

import com.vibe.acceptance.dto.AcceptanceStandardQueryDTO;
import com.vibe.acceptance.dto.AcceptanceStandardSaveDTO;
import com.vibe.acceptance.vo.AcceptanceStandardVO;
import com.vibe.common.result.PageResult;

import java.util.List;

/**
 * 验收标准 Service
 *
 * @author vibe
 */
public interface AcceptanceStandardService {

    /**
     * 分页查询验收标准
     */
    PageResult<AcceptanceStandardVO> page(AcceptanceStandardQueryDTO query);

    /**
     * 查询全部启用的验收标准（下拉选择用）
     */
    List<AcceptanceStandardVO> listEnabled();

    /**
     * 获取验收标准详情（含检查项列表）
     */
    AcceptanceStandardVO getDetail(Long id);

    /**
     * 创建验收标准（含检查项）
     */
    Long save(AcceptanceStandardSaveDTO dto);

    /**
     * 更新验收标准（含检查项，全量替换）
     */
    void update(Long id, AcceptanceStandardSaveDTO dto);

    /**
     * 删除验收标准（逻辑删除）
     */
    void delete(Long id);
}
