package com.vibe.acceptance.service;

import com.vibe.acceptance.dto.AcceptanceTaskActionDTO;
import com.vibe.acceptance.dto.AcceptanceTaskCreateDTO;
import com.vibe.acceptance.dto.AcceptanceTaskQueryDTO;
import com.vibe.acceptance.vo.AcceptanceTaskVO;
import com.vibe.acceptance.vo.AcceptanceTestRecordVO;
import com.vibe.common.result.PageResult;

import java.util.List;

/**
 * 验收任务 Service
 *
 * @author vibe
 */
public interface AcceptanceTaskService {

    /**
     * 分页查询验收任务
     */
    PageResult<AcceptanceTaskVO> page(AcceptanceTaskQueryDTO query);

    /**
     * 获取验收任务详情
     */
    AcceptanceTaskVO getDetail(Long id);

    /**
     * 创建验收任务
     */
    Long create(AcceptanceTaskCreateDTO dto);

    /**
     * 更新验收任务
     */
    void update(Long id, AcceptanceTaskCreateDTO dto);

    /**
     * 删除验收任务（仅 DRAFT 状态可删）
     */
    void delete(Long id);

    /**
     * PM 提交验收申请（DRAFT → APPLIED）
     */
    void apply(AcceptanceTaskActionDTO dto);

    /**
     * 内部技术审核（APPLIED → INTERNAL_AUDITED / REJECTED）
     */
    void internalAudit(AcceptanceTaskActionDTO dto);

    /**
     * 发起客户签核（INTERNAL_AUDITED → CUSTOMER_SIGNING）
     */
    void startCustomerSign(Long taskId);

    /**
     * 客户签核（CUSTOMER_SIGNING → COMPLETED / REJECTED）
     */
    void customerSign(AcceptanceTaskActionDTO dto);

    /**
     * 查询任务关联的测试记录
     */
    List<AcceptanceTestRecordVO> listTestRecords(Long taskId);
}
