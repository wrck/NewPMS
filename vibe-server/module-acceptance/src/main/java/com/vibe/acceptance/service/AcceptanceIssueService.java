package com.vibe.acceptance.service;

import com.vibe.acceptance.dto.AcceptanceIssueQueryDTO;
import com.vibe.acceptance.dto.AcceptanceIssueSaveDTO;
import com.vibe.acceptance.vo.AcceptanceIssueVO;
import com.vibe.common.result.PageResult;

/**
 * 验收遗留问题 Service
 *
 * @author vibe
 */
public interface AcceptanceIssueService {

    /**
     * 分页查询遗留问题
     */
    PageResult<AcceptanceIssueVO> page(AcceptanceIssueQueryDTO query);

    /**
     * 获取遗留问题详情
     */
    AcceptanceIssueVO getDetail(Long id);

    /**
     * 创建遗留问题
     */
    Long save(AcceptanceIssueSaveDTO dto);

    /**
     * 更新遗留问题
     */
    void update(Long id, AcceptanceIssueSaveDTO dto);

    /**
     * 删除遗留问题
     */
    void delete(Long id);

    /**
     * 指派整改责任人
     */
    void assign(Long id, Long assigneeId);

    /**
     * 标记整改完成
     */
    void resolve(Long id);

    /**
     * 闭环确认
     */
    void close(Long id);
}
