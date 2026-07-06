package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysFeedbackDTO;
import com.vibe.system.dto.SysFeedbackHandleDTO;
import com.vibe.system.dto.SysFeedbackQueryDTO;
import com.vibe.system.vo.SysFeedbackVO;

/**
 * 反馈与工单服务
 *
 * @author vibe
 */
public interface SysFeedbackService {

    /**
     * 提交反馈（任意已登录用户）
     *
     * @param dto         反馈内容
     * @param submitterId 提交人 ID（来自登录上下文）
     */
    Long submit(SysFeedbackDTO dto, Long submitterId);

    /**
     * 管理员分页查询全部反馈
     */
    PageResult<SysFeedbackVO> pageAll(SysFeedbackQueryDTO query);

    /**
     * 当前用户提交的反馈分页查询
     *
     * @param query       查询条件（忽略 submitterId 参数，强制覆盖为当前用户）
     * @param submitterId 当前用户 ID
     */
    PageResult<SysFeedbackVO> pageMy(SysFeedbackQueryDTO query, Long submitterId);

    /**
     * 处理反馈（管理员）
     *
     * @param feedbackId 反馈 ID
     * @param dto         处理参数（状态 + 备注）
     * @param handlerId   处理人 ID
     */
    void handle(Long feedbackId, SysFeedbackHandleDTO dto, Long handlerId);
}
