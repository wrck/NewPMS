package com.vibe.collaboration.mapper;

import com.vibe.collaboration.bo.PhaseDeliverableRow;
import com.vibe.collaboration.vo.CustomerAcceptanceTaskVO;
import com.vibe.collaboration.vo.CustomerCutoverPlanVO;
import com.vibe.collaboration.vo.CustomerMessageVO;
import com.vibe.collaboration.vo.CustomerProjectVO;
import com.vibe.collaboration.vo.CustomerTodoVO;
import com.vibe.collaboration.vo.PhaseTimelineVO;
import com.vibe.collaboration.vo.ProjectProgressVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户门户 Mapper
 *
 * <p>提供客户 H5 端项目进度/文档/审批/消息查询所需的只读查询。
 * 通过 XML 中跨表 SQL 直接查询 cutover_plan / acceptance_task / customer_message，
 * 避免模块间循环依赖。</p>
 *
 * @author vibe
 */
@Mapper
public interface CustomerPortalMapper {

    /**
     * 查询客户关联的项目列表（脱敏字段）。
     */
    List<CustomerProjectVO> selectCustomerProjects(@Param("customerId") Long customerId);

    /**
     * 查询项目整体进度信息。
     */
    ProjectProgressVO selectProjectProgress(@Param("projectId") Long projectId);

    /**
     * 查询项目阶段时间线。
     */
    List<PhaseTimelineVO> selectPhaseTimeline(@Param("projectId") Long projectId);

    /**
     * 查询项目可下载文档。
     */
    List<PhaseDeliverableRow> selectProjectDocuments(@Param("projectId") Long projectId);

    /**
     * 查询项目的客户ID（数据隔离校验）。
     */
    Long selectCustomerIdByProjectId(@Param("projectId") Long projectId);

    /* ============ 割接审批（3.2） ============ */

    /**
     * 根据客户签核 token 查询割接方案基础信息（含 projectId 用于归属校验）。
     *
     * <p>仅返回 PENDING_CUSTOMER_APPROVAL 状态的方案。</p>
     *
     * @param token 客户签核链接 token
     * @return 割接方案 VO（不含 steps）；token 无效/方案不存在时返回 null
     */
    CustomerCutoverPlanVO selectCutoverPlanByToken(@Param("token") String token);

    /**
     * 查询割接方案下的步骤列表（客户脱敏视图）。
     *
     * @param planId 割接方案ID
     * @return 步骤列表
     */
    List<CustomerCutoverPlanVO.CustomerCutoverStepVO> selectCutoverStepsByPlanId(@Param("planId") Long planId);

    /**
     * 查询项目下所有待客户审批的割接方案（用于待办列表）。
     *
     * @param projectId 项目ID
     * @return 割接方案列表（仅 PENDING_CUSTOMER_APPROVAL 状态）
     */
    List<CustomerCutoverPlanVO> selectCutoverPlansPendingApproval(@Param("projectId") Long projectId);

    /**
     * 客户提交割接审批结果（直接 UPDATE cutover_plan）。
     *
     * @param planId   方案ID
     * @param result   APPROVED/REJECTED
     * @param signUser 客户签核人姓名
     * @param remark   审批意见
     * @param signTime 签核时间
     * @param newStatus 新状态 CUSTOMER_APPROVED/CUSTOMER_REJECTED
     * @return 影响行数
     */
    int updateCutoverPlanCustomerApproval(@Param("planId") Long planId,
                                          @Param("result") String result,
                                          @Param("signUser") String signUser,
                                          @Param("remark") String remark,
                                          @Param("signTime") LocalDateTime signTime,
                                          @Param("newStatus") String newStatus);

    /* ============ 验收签核（3.3） ============ */

    /**
     * 根据客户签核 token 查询验收任务基础信息。
     *
     * <p>仅返回 CUSTOMER_SIGNING 状态的任务。</p>
     *
     * @param token 客户签核链接 token
     * @return 验收任务 VO（不含 testRecords）；token 无效时返回 null
     */
    CustomerAcceptanceTaskVO selectAcceptanceTaskByToken(@Param("token") String token);

    /**
     * 查询验收任务的测试记录列表（客户脱敏视图）。
     *
     * @param taskId 验收任务ID
     * @return 测试记录列表
     */
    List<CustomerAcceptanceTaskVO.CustomerTestRecordVO> selectAcceptanceTestRecords(@Param("taskId") Long taskId);

    /**
     * 查询项目下所有待客户签核的验收任务（用于待办列表）。
     *
     * @param projectId 项目ID
     * @return 验收任务列表（仅 CUSTOMER_SIGNING 状态）
     */
    List<CustomerAcceptanceTaskVO> selectAcceptanceTasksPendingSign(@Param("projectId") Long projectId);

    /**
     * 客户提交验收签核结果（直接 UPDATE acceptance_task）。
     *
     * @param taskId    任务ID
     * @param result    PASS/CONDITIONAL_PASS/REJECT
     * @param signUser  客户签核人姓名
     * @param remark    签核意见
     * @param signTime  签核时间
     * @param newStatus 新状态 COMPLETED/REJECTED
     * @return 影响行数
     */
    int updateAcceptanceTaskCustomerSign(@Param("taskId") Long taskId,
                                         @Param("result") String result,
                                         @Param("signUser") String signUser,
                                         @Param("remark") String remark,
                                         @Param("signTime") LocalDateTime signTime,
                                         @Param("newStatus") String newStatus);

    /* ============ 客户消息（3.5） ============ */

    /**
     * 查询客户的未读消息列表。
     *
     * @param customerId 客户ID
     * @return 消息列表（按创建时间倒序）
     */
    List<CustomerMessageVO> selectCustomerMessages(@Param("customerId") Long customerId);

    /**
     * 统计客户未读消息数。
     *
     * @param customerId 客户ID
     * @return 未读消息数
     */
    int countUnreadMessages(@Param("customerId") Long customerId);

    /**
     * 标记消息为已读。
     *
     * @param messageId  消息ID
     * @param customerId 客户ID（数据隔离）
     * @return 影响行数
     */
    int markMessageRead(@Param("messageId") Long messageId, @Param("customerId") Long customerId);

    /**
     * 标记客户所有未读消息为已读。
     *
     * @param customerId 客户ID
     * @return 影响行数
     */
    int markAllMessagesRead(@Param("customerId") Long customerId);

    /**
     * 插入客户消息。
     *
     * @param message 消息 VO（需填充所有字段）
     * @return 影响行数
     */
    int insertCustomerMessage(CustomerMessageVO message);
}
