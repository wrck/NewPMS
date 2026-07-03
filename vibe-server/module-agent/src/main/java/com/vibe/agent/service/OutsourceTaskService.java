package com.vibe.agent.service;

import com.vibe.agent.dto.OutsourceTaskActionDTO;
import com.vibe.agent.dto.OutsourceTaskCreateDTO;
import com.vibe.agent.dto.OutsourceTaskQueryDTO;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.common.result.PageResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 转包任务服务（代理商管理核心服务）
 *
 * <p>负责转包任务的全生命周期管理：</p>
 * <ul>
 *   <li>创建转包任务（PM 指定代理商、任务范围、截止日期）</li>
 *   <li>代理商接单/拒绝（PENDING → ACCEPTED/REJECTED）</li>
 *   <li>代理商指派工程师（ACCEPTED → IN_PROGRESS）</li>
 *   <li>代理商提交交付物（IN_PROGRESS → SUBMITTED，在 DeliverableService 中触发）</li>
 *   <li>PM 审核通过/退回（SUBMITTED → CONFIRMED/RETURNED）</li>
 *   <li>退回后重新提交（RETURNED → IN_PROGRESS）</li>
 *   <li>超期自动标记（定时任务，任意非终态 → OVERDUE）</li>
 * </ul>
 *
 * <p><b>数据权限隔离：</b>列表查询通过 {@code @DataPermission} 注解，
 * AGENT_ADMIN 仅看本公司任务（agent_company_id = tenantId），
 * AGENT_ENGINEER 仅看分配给自己的任务（agent_engineer_id = userId）。</p>
 *
 * <p><b>状态机：</b>所有状态流转均通过 {@link #validateTransition} 校验，
 * 非法流转抛出 {@code STATE_TRANSITION_INVALID}（40902）错误。</p>
 *
 * @author vibe
 */
public interface OutsourceTaskService {

    /**
     * 创建转包任务（PM 发起）。
     *
     * <p>状态初始化为 PENDING，关联 project_task_id。</p>
     *
     * @return 转包任务ID
     */
    Long create(OutsourceTaskCreateDTO dto);

    /**
     * 分页查询转包任务（含项目名/任务名/代理商名关联）。
     *
     * <p>数据权限：AGENT_ADMIN 仅看本公司任务，AGENT_ENGINEER 仅看自己任务。
     * 对代理商角色 VO 脱敏（屏蔽客户/合同/成本字段）。</p>
     */
    PageResult<OutsourceTaskVO> page(OutsourceTaskQueryDTO query);

    /**
     * 查询转包任务详情（含关联信息）。
     *
     * <p>对代理商角色 VO 脱敏。</p>
     */
    OutsourceTaskVO getDetail(Long id);

    /**
     * 代理商接单（PENDING → ACCEPTED）。
     *
     * <p>仅 AGENT_ADMIN 可操作，且任务 agent_company_id 必须等于当前用户 tenantId。</p>
     */
    void accept(Long taskId);

    /**
     * 代理商拒绝接单（PENDING → REJECTED）。
     *
     * <p>仅 AGENT_ADMIN 可操作，需填写拒绝原因。</p>
     */
    void reject(Long taskId, OutsourceTaskActionDTO dto);

    /**
     * 代理商指派工程师（ACCEPTED → IN_PROGRESS）。
     *
     * <p>仅 AGENT_ADMIN 可操作，工程师必须属于本公司且为启用状态。</p>
     */
    void assignEngineer(Long taskId, OutsourceTaskActionDTO dto);

    /**
     * 代理商提交交付物后触发状态流转（IN_PROGRESS/RETURNED → SUBMITTED）。
     *
     * <p>由 {@code OutsourceDeliverableService.submit} 调用，
     * 增加 submit_count，清空 rejectReason。</p>
     */
    void markSubmitted(Long taskId);

    /**
     * PM 审核通过（SUBMITTED → CONFIRMED）。
     *
     * <p>仅 PM/SUPER_ADMIN 可操作，记录确认人与确认时间。</p>
     */
    void confirm(Long taskId);

    /**
     * PM 审核退回（SUBMITTED → RETURNED）。
     *
     * <p>仅 PM/SUPER_ADMIN 可操作，submit_count +1，填写退回原因。</p>
     */
    void returnTask(Long taskId, OutsourceTaskActionDTO dto);

    /**
     * 代理商重新提交（RETURNED → IN_PROGRESS）。
     *
     * <p>仅 AGENT_ADMIN 可操作，清空退回原因，准备重新提交交付物。</p>
     */
    void resubmit(Long taskId);

    /**
     * 定时任务：扫描超期任务并标记为 OVERDUE。
     *
     * <p>无用户上下文（定时任务），使用 {@code @DataPermission(ignore=true)} 绕过数据权限。
     * 仅更新 deadline &lt; 今天 且状态为非终态的任务。</p>
     *
     * @return 标记的超期任务数
     */
    int markOverdueTasks();

    /**
     * 按代理商公司ID查询任务列表。
     */
    List<OutsourceTaskVO> listByAgentCompany(Long agentCompanyId);

    /**
     * 校验状态流转是否合法，非法则抛出 BusinessException(STATE_TRANSITION_INVALID)。
     *
     * @param currentStatus 当前状态码
     * @param targetStatus  目标状态码
     */
    void validateTransition(String currentStatus, String targetStatus);
}
