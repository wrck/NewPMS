package com.vibe.agent.service;

import com.vibe.agent.dto.DeliverableReviewDTO;
import com.vibe.agent.dto.OutsourceDeliverableDTO;
import com.vibe.agent.dto.OutsourceDeliverableQueryDTO;
import com.vibe.agent.vo.OutsourceDeliverableVO;
import com.vibe.common.result.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 代理商交付物服务
 *
 * <p>提供交付物提交（含必传校验）、PM 审核（通过/退回）、列表查询。</p>
 *
 * @author vibe
 */
public interface OutsourceDeliverableService {

    /**
     * 代理商提交交付物（移动端/H5 调用）。
     *
     * <p>校验规则：</p>
     * <ul>
     *   <li>施工照片（PHOTO）必传，至少 {@code MIN_PHOTO_COUNT} 张</li>
     *   <li>测试记录（TEST_RECORD）必传</li>
     *   <li>签收单（RECEIPT）必传</li>
     * </ul>
     *
     * <p>提交成功后触发任务状态流转：IN_PROGRESS/RETURNED → SUBMITTED。</p>
     *
     * @return 提交的交付物数量
     */
    int submit(OutsourceDeliverableDTO dto);

    /**
     * PM 审核交付物。
     *
     * <p>审核通过：任务 SUBMITTED → CONFIRMED。
     * 审核退回：任务 SUBMITTED → RETURNED，submit_count +1，通知代理商。</p>
     *
     * @param taskId 转包任务ID
     * @param dto    审核结果
     */
    void review(Long taskId, DeliverableReviewDTO dto);

    /**
     * 按任务ID查询交付物列表。
     */
    List<OutsourceDeliverableVO> listByTaskId(Long taskId);

    /**
     * 按任务ID统计各类型交付物数量。
     *
     * @return Map: deliverableType → count
     */
    Map<String, Integer> countByType(Long taskId);
}
