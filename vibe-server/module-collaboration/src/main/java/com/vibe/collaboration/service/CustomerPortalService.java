package com.vibe.collaboration.service;

import com.vibe.collaboration.dto.CustomerAcceptanceSignDTO;
import com.vibe.collaboration.dto.CustomerCutoverApprovalDTO;
import com.vibe.collaboration.vo.CustomerAcceptanceTaskVO;
import com.vibe.collaboration.vo.CustomerCutoverPlanVO;
import com.vibe.collaboration.vo.CustomerMessageVO;
import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.CustomerTodoVO;
import com.vibe.collaboration.vo.DocumentVO;
import com.vibe.collaboration.vo.ProjectProgressVO;

import java.util.List;

/**
 * 客户门户服务
 *
 * <p>提供客户 H5 端的项目进度查看/文档下载/割接审批/验收签核/消息通知能力。
 * 所有方法均强制数据隔离：CUSTOMER 角色仅能访问自己关联的项目。</p>
 *
 * @author vibe
 */
public interface CustomerPortalService {

    /* ============ 3.1 进度查看（已实现） ============ */
    List<CustomerProjectVO> getMyProjects();
    ProjectProgressVO getProjectProgress(Long projectId);
    List<DocumentVO> getProjectDocuments(Long projectId);

    /* ============ 3.2 割接审批 ============ */

    /**
     * 通过 token 查看割接方案详情（含步骤，脱敏）。
     *
     * <p>token 是 PM 在创建/审批割接方案时生成的客户签核链接 token，
     * 客户无需登录即可访问，但仅能查看 PENDING_CUSTOMER_APPROVAL 状态的方案。</p>
     *
     * @param token 客户签核链接 token
     * @return 割接方案详情（含步骤）
     */
    CustomerCutoverPlanVO getCutoverPlanByToken(String token);

    /**
     * 客户提交割接审批结果。
     *
     * <p>校验 token 有效性 + 项目归属当前客户 + 当前状态为 PENDING_CUSTOMER_APPROVAL，
     * 通过后状态变为 CUSTOMER_APPROVED 或 CUSTOMER_REJECTED。</p>
     *
     * @param dto 审批 DTO
     */
    void submitCutoverApproval(CustomerCutoverApprovalDTO dto);

    /* ============ 3.3 验收签核 ============ */

    /**
     * 通过 token 查看验收任务详情（含测试记录，脱敏）。
     *
     * @param token 客户签核链接 token
     * @return 验收任务详情
     */
    CustomerAcceptanceTaskVO getAcceptanceTaskByToken(String token);

    /**
     * 客户提交验收签核结果。
     *
     * @param dto 签核 DTO
     */
    void submitAcceptanceSign(CustomerAcceptanceSignDTO dto);

    /* ============ 3.5 消息通知 ============ */

    /**
     * 查询客户的消息列表。
     */
    List<CustomerMessageVO> getMyMessages();

    /**
     * 统计未读消息数。
     */
    int countUnreadMessages();

    /**
     * 标记消息为已读。
     */
    void markMessageRead(Long messageId);

    /**
     * 标记所有未读消息为已读。
     */
    void markAllMessagesRead();

    /* ============ 待办列表（聚合） ============ */

    /**
     * 查询当前登录客户的待办事项（待审批的割接方案 + 待签核的验收任务）。
     *
     * <p>需要登录态，遍历客户关联的所有项目，聚合每个项目中 PENDING_CUSTOMER_APPROVAL 状态的割接方案
     * 和 CUSTOMER_SIGNING 状态的验收任务。</p>
     */
    List<CustomerTodoVO> getMyTodos();
}
