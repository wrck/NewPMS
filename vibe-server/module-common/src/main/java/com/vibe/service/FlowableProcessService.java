package com.vibe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.Task;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flowable 工作流统一服务封装
 *
 * <p>本类对 Flowable 7 的 {@link RuntimeService}/{@link TaskService}/
 * {@link HistoryService}/{@link RepositoryService} 进行门面封装，
 * 为业务 Controller 提供统一的流程操作 API：</p>
 *
 * <ul>
 *   <li>{@link #startProcess(String, String, Map)}：启动流程（businessKey 关联业务实体）</li>
 *   <li>{@link #approve(String, Map)}：审批通过（完成任务 + 设置 approved=true）</li>
 *   <li>{@link #reject(String, String)}：拒绝/回退（完成任务 + 设置 approved=false + reason）</li>
 *   <li>{@link #queryTasks(String)}：查询用户待办（assignee + 候选组）</li>
 *   <li>{@link #queryHistory(String)}：查询流程历史活动轨迹</li>
 *   <li>{@link #withdraw(String)}：撤回流程（取消运行中实例）</li>
 *   <li>{@link #urge(String)}：催办（基于 Redis 1 小时去重，避免重复催办）</li>
 * </ul>
 *
 * <p><b>变量约定：</b>BPMN 流程定义中通过条件表达式
 * {@code ${approved == true}} / {@code ${approved == false}} 进行分支路由，
 * 因此审批/拒绝时必须设置 {@code approved} 流程变量。</p>
 *
 * <p><b>事务：</b>本服务的所有写操作（启动/审批/拒绝/撤回）应在调用方的
 * {@code @Transactional} 上下文中执行，与 MyBatis-Plus 业务操作共用同一事务。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowableProcessService {

    /** 流程变量：审批结果（true=通过 / false=拒绝） */
    public static final String VAR_APPROVED = "approved";
    /** 流程变量：拒绝原因 */
    public static final String VAR_REJECT_REASON = "rejectReason";
    /** 流程变量：发起人 userId（BPMN 中 assignee=${initiator}） */
    public static final String VAR_INITIATOR = "initiator";
    /** 流程变量：会签审批人列表（BPMN 中 flowable:collection=${approvers}） */
    public static final String VAR_APPROVERS = "approvers";

    /** 催办 Redis 去重 key 前缀（vibe:flowable:urge:{taskId}），TTL 1 小时 */
    private static final String URGE_REDIS_KEY_PREFIX = "vibe:flowable:urge:";
    private static final Duration URGE_DEDUP_TTL = Duration.ofHours(1);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final StringRedisTemplate stringRedisTemplate;

    /* ============ 启动流程 ============ */

    /**
     * 启动流程实例（关联业务实体作为 businessKey）。
     *
     * <p>调用方应将业务实体 ID（如验收任务 ID、割接方案 ID）作为 businessKey 传入，
     * 便于后续按业务实体反查流程实例。</p>
     *
     * @param processDefinitionKey 流程定义 key（如 acceptance / cutover / outsource / change）
     * @param businessKey           业务实体 ID（字符串形式）
     * @param variables             启动变量（必须包含 initiator=发起人 userId；
     *                              会签节点需 approvers=审批人 userId 列表）
     * @return 流程实例（含 processInstanceId）
     */
    public ProcessInstance startProcess(String processDefinitionKey,
                                        String businessKey,
                                        Map<String, Object> variables) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        log.info("[Flowable] 启动流程：definitionKey={}, businessKey={}, variables={}",
                processDefinitionKey, businessKey, variables.keySet());
        return runtimeService.startProcessInstanceByKey(
                processDefinitionKey, businessKey, variables);
    }

    /* ============ 审批通过 ============ */

    /**
     * 审批通过：完成当前任务并设置 approved=true 流程变量。
     *
     * <p>调用方可在 variables 中追加业务相关变量（如审批意见、附件 ID），
     * 这些变量会作为流程变量持久化到 Flowable 历史表中。</p>
     *
     * @param taskId    Flowable 任务 ID（业务层从 queryTasks 获取）
     * @param variables 附加流程变量（可为 null；本方法会强制设置 approved=true）
     */
    public void approve(String taskId, Map<String, Object> variables) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put(VAR_APPROVED, Boolean.TRUE);
        log.info("[Flowable] 审批通过：taskId={}, variables={}", taskId, variables.keySet());
        taskService.complete(taskId, variables);
    }

    /* ============ 拒绝/回退 ============ */

    /**
     * 拒绝/回退：完成当前任务并设置 approved=false + 拒绝原因。
     *
     * <p>BPMN 中定义的 {@code ${approved == false}} 出口路由将触发，
     * 流程将回退到上一节点或终止流程（视具体 BPMN 设计）。</p>
     *
     * @param taskId Flowable 任务 ID
     * @param reason 拒绝原因（写入流程变量 rejectReason，便于审计）
     */
    public void reject(String taskId, String reason) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(VAR_APPROVED, Boolean.FALSE);
        variables.put(VAR_REJECT_REASON, reason);
        log.info("[Flowable] 拒绝/回退：taskId={}, reason={}", taskId, reason);
        taskService.complete(taskId, variables);
    }

    /* ============ 查询待办 ============ */

    /**
     * 查询用户待办任务（assignee = userId 或候选用户包含 userId）。
     *
     * <p>使用 Flowable 的 {@code taskCandidateOrAssigned} 一并查询：
     * <ul>
     *   <li>已分配给当前 userId 的任务（taskAssignee = userId）</li>
     *   <li>候选用户/候选组中包含当前 userId 的任务（用户可签收）</li>
     * </ul>
     * 返回结果按创建时间倒序排列。</p>
     *
     * @param userId 用户 ID（字符串形式）
     * @return 待办任务列表
     */
    public List<Task> queryTasks(String userId) {
        return taskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime().desc()
                .list();
    }

    /* ============ 查询历史 ============ */

    /**
     * 查询流程历史活动轨迹（按时间正序）。
     *
     * <p>返回所有历史活动（含 userTask / exclusiveGateway / startEvent / endEvent），
     * 便于前端渲染流程进度时间线。</p>
     *
     * @param processInstanceId 流程实例 ID
     * @return 历史活动列表
     */
    public List<HistoricActivityInstance> queryHistory(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
    }

    /* ============ 撤回 ============ */

    /**
     * 撤回流程：取消运行中的流程实例。
     *
     * <p>撤回语义：发起人取消已提交的流程，使其回到草稿状态。
     * 本实现通过 {@link RuntimeService#deleteProcessInstance} 终止流程实例，
     * 业务层可重新发起或修改后再次提交。</p>
     *
     * <p>如需"回到上一节点"而非"终止流程"的语义，可改用
     * {@code runtimeService.createChangeActivityStateBuilder()} 进行节点回退，
     * 当前实现采用更简单可靠的终止语义。</p>
     *
     * @param processInstanceId 流程实例 ID
     * @return true 表示撤回成功；false 表示流程已结束或不存在
     */
    public boolean withdraw(String processInstanceId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instance == null || instance.isEnded()) {
            log.warn("[Flowable] 撤回失败：流程实例已结束或不存在，processInstanceId={}", processInstanceId);
            return false;
        }
        runtimeService.deleteProcessInstance(processInstanceId, "用户撤回");
        log.info("[Flowable] 撤回成功：processInstanceId={}", processInstanceId);
        return true;
    }

    /* ============ 催办 ============ */

    /**
     * 催办当前任务的处理人。
     *
     * <p>基于 Redis 实现 1 小时去重，避免对同一任务的重复催办打扰处理人。
     * 当前实现仅记录催办日志，后续可对接消息通知引擎（飞书/钉钉/站内信）。</p>
     *
     * @param taskId Flowable 任务 ID
     * @return true 表示催办成功（首次或去重窗口外）；false 表示 1 小时内已催办过
     */
    public boolean urge(String taskId) {
        String dedupKey = URGE_REDIS_KEY_PREFIX + taskId;
        Boolean firstTime = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", URGE_DEDUP_TTL);
        if (Boolean.FALSE.equals(firstTime)) {
            log.info("[Flowable] 催办去重命中：taskId={}（1 小时内已催办）", taskId);
            return false;
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("[Flowable] 催办失败：任务不存在或已完成，taskId={}", taskId);
            return false;
        }
        String assignee = task.getAssignee();
        log.info("[Flowable] 催办触发：taskId={}, taskName={}, assignee={}",
                taskId, task.getName(), assignee);
        // TODO: 后续对接消息通知引擎（com.vibe.system.service.SysNoticeService）
        //       发送站内信 / 飞书 / 钉钉通知 assignee
        return true;
    }

    /* ============ 辅助查询 ============ */

    /**
     * 按 businessKey 查询运行中的流程实例。
     *
     * <p>业务层在审批回调时，可凭业务实体 ID（如验收任务 ID）
     * 反查关联的流程实例，进而定位当前活动任务。</p>
     *
     * @param processDefinitionKey 流程定义 key
     * @param businessKey           业务实体 ID
     * @return 流程实例；不存在返回 null
     */
    public ProcessInstance getProcessInstanceByBusinessKey(String processDefinitionKey,
                                                           String businessKey) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .processInstanceBusinessKey(businessKey)
                .active()
                .singleResult();
    }

    /**
     * 按 businessKey 查询流程实例下所有活动任务（按创建时间升序）。
     *
     * <p>用于业务层在审批回调时凭业务实体 ID 定位 Flowable 任务 ID。
     * 多实例会签场景下可能返回多个任务。</p>
     *
     * @param processDefinitionKey 流程定义 key
     * @param businessKey           业务实体 ID
     * @return 活动任务列表；流程不存在时返回空列表
     */
    public List<Task> findActiveTasksByBusinessKey(String processDefinitionKey, String businessKey) {
        ProcessInstance pi = getProcessInstanceByBusinessKey(processDefinitionKey, businessKey);
        if (pi == null) {
            return List.of();
        }
        return taskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .active()
                .orderByTaskCreateTime().asc()
                .list();
    }

    /**
     * 按 businessKey + assignee 查询活动任务（适用于已完成会签其中一个实例的场景）。
     *
     * @param processDefinitionKey 流程定义 key
     * @param businessKey           业务实体 ID
     * @param assignee              任务办理人 userId（字符串）
     * @return 任务；不存在返回 null
     */
    public Task findActiveTaskByAssignee(String processDefinitionKey,
                                         String businessKey,
                                         String assignee) {
        ProcessInstance pi = getProcessInstanceByBusinessKey(processDefinitionKey, businessKey);
        if (pi == null) {
            return null;
        }
        return taskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .taskAssignee(assignee)
                .active()
                .singleResult();
    }

    /**
     * 查询流程定义信息（含名称、版本、部署 ID 等）。
     *
     * @param processDefinitionKey 流程定义 key
     * @return 流程定义；不存在返回 null
     */
    public ProcessDefinition getProcessDefinition(String processDefinitionKey) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .latestVersion()
                .singleResult();
    }
}
