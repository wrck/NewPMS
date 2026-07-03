package com.vibe.agent.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.OutsourceTaskActionDTO;
import com.vibe.agent.dto.OutsourceTaskCreateDTO;
import com.vibe.agent.dto.OutsourceTaskQueryDTO;
import com.vibe.agent.entity.OutsourceTaskEntity;
import com.vibe.agent.enums.OutsourceTaskStatusEnum;
import com.vibe.agent.mapper.OutsourceTaskMapper;
import com.vibe.agent.service.AgentCompanyService;
import com.vibe.agent.service.AgentEngineerService;
import com.vibe.agent.service.OutsourceTaskService;
import com.vibe.agent.vo.OutsourceTaskVO;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 转包任务服务实现（代理商管理核心服务）
 *
 * <p><b>状态机：</b>所有状态流转通过 {@link OutsourceTaskStatusEnum#canTransitionTo} 校验，
 * 非法流转抛出 {@code STATE_TRANSITION_INVALID}（40902）错误。</p>
 *
 * <p><b>数据权限隔离：</b></p>
 * <ul>
 *   <li>列表查询：{@link OutsourceTaskMapper#selectTaskPage} 方法上的
 *       {@code @DataPermission(table = "t", agentField = "agent_company_id")}
 *       注解，AGENT_ADMIN 仅看本公司任务（agent_company_id = tenantId），
 *       AGENT_ENGINEER 仅看分配给自己的任务（agent_engineer_id = userId）</li>
 *   <li>VO 脱敏：对代理商角色屏蔽客户/合同/成本字段（调用 VO.desensitizeForAgent()）</li>
 * </ul>
 *
 * <p><b>乐观锁：</b>outsource_task 表含 version 字段，MyBatis-Plus 自动处理乐观锁
 * （OptimisticLockerInnerInterceptor），并发更新冲突时抛出异常。</p>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutsourceTaskServiceImpl implements OutsourceTaskService {

    private final OutsourceTaskMapper outsourceTaskMapper;
    private final AgentCompanyService agentCompanyService;
    private final AgentEngineerService agentEngineerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OutsourceTaskCreateDTO dto) {
        // 校验代理商公司存在且为活跃状态
        agentCompanyService.validateActive(dto.getAgentCompanyId());

        OutsourceTaskEntity entity = new OutsourceTaskEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setTaskId(dto.getTaskId());
        entity.setAgentCompanyId(dto.getAgentCompanyId());
        entity.setTaskScope(dto.getTaskScope());
        entity.setDeadline(dto.getDeadline());
        entity.setStatus(AgentConstant.TASK_PENDING);
        entity.setSubmitCount(0);
        outsourceTaskMapper.insert(entity);
        log.info("创建转包任务: id={}, projectId={}, taskId={}, agentCompanyId={}",
                entity.getId(), entity.getProjectId(), entity.getTaskId(), entity.getAgentCompanyId());
        return entity.getId();
    }

    @Override
    public PageResult<OutsourceTaskVO> page(OutsourceTaskQueryDTO query) {
        IPage<OutsourceTaskVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        // @DataPermission 在 Mapper 方法上：AGENT_ADMIN 仅看本公司任务，AGENT_ENGINEER 仅看自己任务
        IPage<OutsourceTaskVO> result = outsourceTaskMapper.selectTaskPage(page, query);
        List<OutsourceTaskVO> records = result.getRecords();
        if (records != null) {
            records.forEach(this::desensitizeIfAgent);
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public OutsourceTaskVO getDetail(Long id) {
        OutsourceTaskVO vo = outsourceTaskMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.of(ResultCode.OUTSOURCE_TASK_NOT_FOUND);
        }
        desensitizeIfAgent(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long taskId) {
        OutsourceTaskEntity task = loadAndCheckOwnership(taskId);
        validateTransition(task.getStatus(), AgentConstant.TASK_ACCEPTED);

        OutsourceTaskEntity update = new OutsourceTaskEntity();
        update.setId(taskId);
        update.setVersion(task.getVersion());
        update.setStatus(AgentConstant.TASK_ACCEPTED);
        outsourceTaskMapper.updateById(update);
        log.info("代理商接单: taskId={}, agentCompanyId={}", taskId, task.getAgentCompanyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, OutsourceTaskActionDTO dto) {
        OutsourceTaskEntity task = loadAndCheckOwnership(taskId);
        validateTransition(task.getStatus(), AgentConstant.TASK_REJECTED);

        OutsourceTaskEntity update = new OutsourceTaskEntity();
        update.setId(taskId);
        update.setVersion(task.getVersion());
        update.setStatus(AgentConstant.TASK_REJECTED);
        update.setRejectReason(dto.getReason());
        outsourceTaskMapper.updateById(update);
        log.info("代理商拒绝接单: taskId={}, reason={}", taskId, dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignEngineer(Long taskId, OutsourceTaskActionDTO dto) {
        if (dto.getAgentEngineerId() == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "代理商工程师ID不能为空");
        }
        OutsourceTaskEntity task = loadAndCheckOwnership(taskId);
        validateTransition(task.getStatus(), AgentConstant.TASK_IN_PROGRESS);

        // 校验工程师属于本公司且为启用状态
        agentEngineerService.validateActive(dto.getAgentEngineerId(), task.getAgentCompanyId());

        OutsourceTaskEntity update = new OutsourceTaskEntity();
        update.setId(taskId);
        update.setVersion(task.getVersion());
        update.setStatus(AgentConstant.TASK_IN_PROGRESS);
        update.setAgentEngineerId(dto.getAgentEngineerId());
        outsourceTaskMapper.updateById(update);
        log.info("代理商指派工程师: taskId={}, engineerId={}", taskId, dto.getAgentEngineerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSubmitted(Long taskId) {
        OutsourceTaskEntity task = loadAndCheckOwnership(taskId);
        // IN_PROGRESS 或 RETURNED 状态均可流转到 SUBMITTED
        validateTransition(task.getStatus(), AgentConstant.TASK_SUBMITTED);

        OutsourceTaskEntity update = new OutsourceTaskEntity();
        update.setId(taskId);
        update.setVersion(task.getVersion());
        update.setStatus(AgentConstant.TASK_SUBMITTED);
        update.setSubmitCount(task.getSubmitCount() == null ? 1 : task.getSubmitCount() + 1);
        update.setRejectReason(null);
        outsourceTaskMapper.updateById(update);
        log.info("代理商提交交付物: taskId={}, submitCount={}", taskId, update.getSubmitCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long taskId) {
        OutsourceTaskEntity task = loadTask(taskId);
        validateTransition(task.getStatus(), AgentConstant.TASK_CONFIRMED);

        OutsourceTaskEntity update = new OutsourceTaskEntity();
        update.setId(taskId);
        update.setVersion(task.getVersion());
        update.setStatus(AgentConstant.TASK_CONFIRMED);
        update.setConfirmedBy(UserContextHolder.getUserId());
        update.setConfirmedTime(LocalDateTime.now());
        outsourceTaskMapper.updateById(update);
        log.info("PM 审核通过: taskId={}, confirmedBy={}", taskId, update.getConfirmedBy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(Long taskId, OutsourceTaskActionDTO dto) {
        OutsourceTaskEntity task = loadTask(taskId);
        validateTransition(task.getStatus(), AgentConstant.TASK_RETURNED);

        OutsourceTaskEntity update = new OutsourceTaskEntity();
        update.setId(taskId);
        update.setVersion(task.getVersion());
        update.setStatus(AgentConstant.TASK_RETURNED);
        update.setRejectReason(dto.getReason());
        outsourceTaskMapper.updateById(update);
        log.info("PM 审核退回: taskId={}, reason={}", taskId, dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long taskId) {
        OutsourceTaskEntity task = loadAndCheckOwnership(taskId);
        validateTransition(task.getStatus(), AgentConstant.TASK_IN_PROGRESS);

        OutsourceTaskEntity update = new OutsourceTaskEntity();
        update.setId(taskId);
        update.setVersion(task.getVersion());
        update.setStatus(AgentConstant.TASK_IN_PROGRESS);
        update.setRejectReason(null);
        outsourceTaskMapper.updateById(update);
        log.info("代理商重新提交（退回后重新进入执行）: taskId={}", taskId);
    }

    @Override
    public int markOverdueTasks() {
        int count = outsourceTaskMapper.markOverdueTasks(LocalDate.now());
        if (count > 0) {
            log.info("定时任务标记超期转包任务: count={}", count);
        }
        return count;
    }

    @Override
    public List<OutsourceTaskVO> listByAgentCompany(Long agentCompanyId) {
        List<OutsourceTaskVO> list = outsourceTaskMapper.selectByAgentCompanyId(agentCompanyId);
        if (list != null) {
            list.forEach(this::desensitizeIfAgent);
        }
        return list;
    }

    @Override
    public void validateTransition(String currentStatus, String targetStatus) {
        OutsourceTaskStatusEnum current = OutsourceTaskStatusEnum.of(currentStatus);
        OutsourceTaskStatusEnum target = OutsourceTaskStatusEnum.of(targetStatus);
        if (current == null || target == null) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    "无效的任务状态: " + currentStatus + " → " + targetStatus);
        }
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(ResultCode.STATE_TRANSITION_INVALID,
                    "任务状态流转非法: " + current.getDescription() + " → " + target.getDescription());
        }
    }

    /* ============ 私有方法 ============ */

    /**
     * 加载任务（含乐观锁版本号），不存在则抛异常。
     */
    private OutsourceTaskEntity loadTask(Long taskId) {
        if (taskId == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "转包任务ID不能为空");
        }
        OutsourceTaskEntity task = outsourceTaskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.of(ResultCode.OUTSOURCE_TASK_NOT_FOUND);
        }
        return task;
    }

    /**
     * 加载任务并校验代理商数据归属（AGENT_ADMIN 只能操作本公司的任务）。
     *
     * <p>数据权限补充校验：虽然查询接口通过 @DataPermission 拦截器隔离，
     * 但写操作（接单/指派/提交等）需要在 Service 层显式校验归属，
     * 防止代理商操作其他公司的任务。</p>
     */
    private OutsourceTaskEntity loadAndCheckOwnership(Long taskId) {
        OutsourceTaskEntity task = loadTask(taskId);
        UserContext ctx = UserContextHolder.get();
        if (ctx != null && ctx.hasRole(AgentConstant.ROLE_AGENT_ADMIN)) {
            Long tenantId = ctx.getTenantId();
            if (tenantId == null || !tenantId.equals(task.getAgentCompanyId())) {
                log.warn("AGENT_ADMIN 越权操作转包任务: userId={}, tenantId={}, taskAgentCompanyId={}, taskId={}",
                        ctx.getUserId(), tenantId, task.getAgentCompanyId(), taskId);
                throw BusinessException.of(ResultCode.DATA_PERMISSION_DENIED);
            }
        }
        return task;
    }

    /**
     * 对代理商角色进行 VO 脱敏（屏蔽客户/合同/成本字段）。
     *
     * <p>判断当前用户为 AGENT_ADMIN / AGENT_ENGINEER 时，调用 VO 的脱敏方法
     * 将敏感字段置为 null。配合全局 Jackson non_null 序列化策略，
     * 敏感字段不会出现在 JSON 响应中。</p>
     */
    private void desensitizeIfAgent(OutsourceTaskVO vo) {
        if (vo == null) {
            return;
        }
        UserContext ctx = UserContextHolder.get();
        if (ctx != null && ctx.hasAnyRole(AgentConstant.ROLE_AGENT_ADMIN, AgentConstant.ROLE_AGENT_ENGINEER)) {
            vo.desensitizeForAgent();
        }
    }
}
