package com.vibe.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.delivery.constant.CutoverConstant;
import com.vibe.delivery.dto.CutoverApprovalDTO;
import com.vibe.delivery.dto.CutoverCompleteDTO;
import com.vibe.delivery.dto.CutoverPlanCreateDTO;
import com.vibe.delivery.dto.CutoverPlanQueryDTO;
import com.vibe.delivery.dto.CutoverStepDTO;
import com.vibe.delivery.dto.CutoverStepExecuteDTO;
import com.vibe.delivery.entity.CutoverExecutionLogEntity;
import com.vibe.delivery.entity.CutoverPlanEntity;
import com.vibe.delivery.entity.CutoverStepEntity;
import com.vibe.delivery.mapper.CutoverExecutionLogMapper;
import com.vibe.delivery.mapper.CutoverPlanMapper;
import com.vibe.delivery.mapper.CutoverStepMapper;
import com.vibe.delivery.service.CutoverPlanService;
import com.vibe.delivery.vo.CutoverExecutionLogVO;
import com.vibe.delivery.vo.CutoverPlanDetailVO;
import com.vibe.delivery.vo.CutoverPlanVO;
import com.vibe.delivery.vo.CutoverStepVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 割接方案 Service 实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CutoverPlanServiceImpl implements CutoverPlanService {

    private final CutoverPlanMapper planMapper;
    private final CutoverStepMapper stepMapper;
    private final CutoverExecutionLogMapper logMapper;

    /* ============ 基础 CRUD ============ */

    @Override
    public PageResult<CutoverPlanVO> page(CutoverPlanQueryDTO query) {
        LambdaQueryWrapper<CutoverPlanEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getProjectId() != null) {
            wrapper.eq(CutoverPlanEntity::getProjectId, query.getProjectId());
        }
        if (query.getPlanName() != null && !query.getPlanName().isBlank()) {
            wrapper.like(CutoverPlanEntity::getPlanName, query.getPlanName());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(CutoverPlanEntity::getStatus, query.getStatus());
        }
        if (query.getDateFrom() != null) {
            wrapper.ge(CutoverPlanEntity::getCutoverDate, query.getDateFrom());
        }
        if (query.getDateTo() != null) {
            wrapper.le(CutoverPlanEntity::getCutoverDate, query.getDateTo());
        }
        if (query.getApplyUserId() != null) {
            wrapper.eq(CutoverPlanEntity::getApplyUserId, query.getApplyUserId());
        }
        wrapper.orderByDesc(CutoverPlanEntity::getCreateTime);

        Page<CutoverPlanEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<CutoverPlanEntity> result = planMapper.selectPage(page, wrapper);

        List<CutoverPlanVO> records = new ArrayList<>();
        for (CutoverPlanEntity e : result.getRecords()) {
            CutoverPlanVO vo = new CutoverPlanVO();
            BeanUtils.copyProperties(e, vo);
            // 填充步骤统计
            int[] counts = countSteps(e.getId());
            vo.setStepCount(counts[0]);
            vo.setCompletedStepCount(counts[1]);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public CutoverPlanDetailVO getDetail(Long id) {
        CutoverPlanEntity entity = planMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("割接方案");
        }
        CutoverPlanDetailVO vo = new CutoverPlanDetailVO();
        BeanUtils.copyProperties(entity, vo);
        // 步骤统计
        int[] counts = countSteps(id);
        vo.setStepCount(counts[0]);
        vo.setCompletedStepCount(counts[1]);
        // 步骤列表
        vo.setSteps(listStepVOs(id));
        // 操作日志
        vo.setLogs(listLogVOs(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CutoverPlanCreateDTO dto) {
        UserContext ctx = requireUserContext();
        // 创建方案
        CutoverPlanEntity entity = new CutoverPlanEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(CutoverConstant.PLAN_STATUS_DRAFT);
        entity.setApplyUserId(ctx.getUserId());
        entity.setApplyTime(LocalDateTime.now());
        planMapper.insert(entity);
        // 创建步骤
        saveSteps(entity.getId(), dto.getSteps(), true);
        // 记录日志
        recordLog(entity.getId(), null, ctx, CutoverConstant.ACTION_CREATE,
                "创建割接方案：" + entity.getPlanName(), CutoverConstant.LOG_LEVEL_INFO);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CutoverPlanCreateDTO dto) {
        CutoverPlanEntity entity = requirePlan(id);
        if (!CutoverConstant.PLAN_STATUS_DRAFT.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的割接方案可修改");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        planMapper.updateById(entity);
        // 重建步骤（草稿阶段可任意修改步骤）
        saveSteps(id, dto.getSteps(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CutoverPlanEntity entity = requirePlan(id);
        if (!CutoverConstant.PLAN_STATUS_DRAFT.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的割接方案可删除");
        }
        planMapper.deleteById(id);
        // 逻辑删除步骤
        LambdaQueryWrapper<CutoverStepEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CutoverStepEntity::getPlanId, id);
        stepMapper.delete(wrapper);
    }

    /* ============ 审批流程 ============ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInternalApproval(Long planId) {
        CutoverPlanEntity entity = requirePlan(planId);
        if (!CutoverConstant.PLAN_STATUS_DRAFT.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态可提交内部审批");
        }
        entity.setStatus(CutoverConstant.PLAN_STATUS_PENDING_INTERNAL_APPROVAL);
        planMapper.updateById(entity);
        UserContext ctx = requireUserContext();
        recordLog(planId, null, ctx, CutoverConstant.ACTION_SUBMIT_INTERNAL_APPROVAL,
                "提交内部审批", CutoverConstant.LOG_LEVEL_INFO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void internalApprove(CutoverApprovalDTO dto) {
        CutoverPlanEntity entity = requirePlan(dto.getPlanId());
        if (!CutoverConstant.PLAN_STATUS_PENDING_INTERNAL_APPROVAL.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅待内部审批状态可审批");
        }
        UserContext ctx = requireUserContext();
        entity.setStatus(CutoverConstant.PLAN_STATUS_INTERNAL_APPROVED);
        entity.setApprovalUserId(ctx.getUserId());
        entity.setApprovalTime(LocalDateTime.now());
        entity.setApprovalRemark(dto.getRemark());
        planMapper.updateById(entity);
        recordLog(dto.getPlanId(), null, ctx, CutoverConstant.ACTION_INTERNAL_APPROVE,
                "内部审批通过" + appendRemark(dto.getRemark()), CutoverConstant.LOG_LEVEL_INFO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void internalReject(CutoverApprovalDTO dto) {
        CutoverPlanEntity entity = requirePlan(dto.getPlanId());
        if (!CutoverConstant.PLAN_STATUS_PENDING_INTERNAL_APPROVAL.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅待内部审批状态可驳回");
        }
        UserContext ctx = requireUserContext();
        entity.setStatus(CutoverConstant.PLAN_STATUS_INTERNAL_REJECTED);
        entity.setApprovalUserId(ctx.getUserId());
        entity.setApprovalTime(LocalDateTime.now());
        entity.setApprovalRemark(dto.getRemark());
        planMapper.updateById(entity);
        recordLog(dto.getPlanId(), null, ctx, CutoverConstant.ACTION_INTERNAL_REJECT,
                "内部审批驳回" + appendRemark(dto.getRemark()), CutoverConstant.LOG_LEVEL_WARN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startCustomerApproval(Long planId) {
        CutoverPlanEntity entity = requirePlan(planId);
        if (!CutoverConstant.PLAN_STATUS_INTERNAL_APPROVED.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅内部审批通过状态可发起客户审批");
        }
        // 生成客户签核链接 token
        String token = UUID.randomUUID().toString().replace("-", "");
        entity.setStatus(CutoverConstant.PLAN_STATUS_PENDING_CUSTOMER_APPROVAL);
        entity.setCustomerSignLink(token);
        planMapper.updateById(entity);
        UserContext ctx = requireUserContext();
        recordLog(planId, null, ctx, CutoverConstant.ACTION_START_CUSTOMER_APPROVAL,
                "发起客户审批，链接token：" + token, CutoverConstant.LOG_LEVEL_INFO);
        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void customerApprove(CutoverApprovalDTO dto) {
        CutoverPlanEntity entity = requirePlan(dto.getPlanId());
        if (!CutoverConstant.PLAN_STATUS_PENDING_CUSTOMER_APPROVAL.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅待客户审批状态可签核");
        }
        entity.setStatus(CutoverConstant.PLAN_STATUS_CUSTOMER_APPROVED);
        entity.setCustomerSignUser(dto.getCustomerSignUser());
        entity.setCustomerSignTime(LocalDateTime.now());
        entity.setCustomerSignResult(CutoverConstant.CUSTOMER_RESULT_APPROVED);
        entity.setCustomerSignRemark(dto.getRemark());
        planMapper.updateById(entity);
        // 客户签核不记录内部操作人，使用客户签核人姓名
        recordLog(dto.getPlanId(), null, null, CutoverConstant.ACTION_CUSTOMER_APPROVE,
                "客户审批通过，签核人：" + dto.getCustomerSignUser() + appendRemark(dto.getRemark()),
                CutoverConstant.LOG_LEVEL_INFO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void customerReject(CutoverApprovalDTO dto) {
        CutoverPlanEntity entity = requirePlan(dto.getPlanId());
        if (!CutoverConstant.PLAN_STATUS_PENDING_CUSTOMER_APPROVAL.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅待客户审批状态可签核");
        }
        entity.setStatus(CutoverConstant.PLAN_STATUS_CUSTOMER_REJECTED);
        entity.setCustomerSignUser(dto.getCustomerSignUser());
        entity.setCustomerSignTime(LocalDateTime.now());
        entity.setCustomerSignResult(CutoverConstant.CUSTOMER_RESULT_REJECTED);
        entity.setCustomerSignRemark(dto.getRemark());
        planMapper.updateById(entity);
        recordLog(dto.getPlanId(), null, null, CutoverConstant.ACTION_CUSTOMER_REJECT,
                "客户审批驳回，签核人：" + dto.getCustomerSignUser() + appendRemark(dto.getRemark()),
                CutoverConstant.LOG_LEVEL_WARN);
    }

    /* ============ 执行流程 ============ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startExecution(Long planId) {
        CutoverPlanEntity entity = requirePlan(planId);
        if (!CutoverConstant.PLAN_STATUS_CUSTOMER_APPROVED.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅客户审批通过状态可开始执行");
        }
        UserContext ctx = requireUserContext();
        entity.setStatus(CutoverConstant.PLAN_STATUS_EXECUTING);
        entity.setActualStartTime(LocalDateTime.now());
        planMapper.updateById(entity);
        recordLog(planId, null, ctx, CutoverConstant.ACTION_START_EXECUTION,
                "开始执行割接", CutoverConstant.LOG_LEVEL_INFO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeStep(CutoverStepExecuteDTO dto) {
        CutoverPlanEntity plan = requirePlan(dto.getPlanId());
        if (!CutoverConstant.PLAN_STATUS_EXECUTING.equals(plan.getStatus())) {
            throw BusinessException.stateNotAllowed("割接方案非执行中状态，无法操作步骤");
        }
        CutoverStepEntity step = requireStep(dto.getStepId(), dto.getPlanId());
        UserContext ctx = requireUserContext();
        LocalDateTime now = LocalDateTime.now();
        if (CutoverConstant.STEP_STATUS_PENDING.equals(step.getStatus())) {
            // PENDING → EXECUTING
            step.setStatus(CutoverConstant.STEP_STATUS_EXECUTING);
            step.setActualStartTime(now);
            if (dto.getExecutionRemark() != null) {
                step.setExecutionRemark(dto.getExecutionRemark());
            }
            stepMapper.updateById(step);
            recordLog(dto.getPlanId(), dto.getStepId(), ctx, CutoverConstant.ACTION_STEP_EXECUTE,
                    "开始执行步骤：" + step.getStepName(), CutoverConstant.LOG_LEVEL_INFO);
        } else if (CutoverConstant.STEP_STATUS_EXECUTING.equals(step.getStatus())) {
            // EXECUTING → COMPLETED
            step.setStatus(CutoverConstant.STEP_STATUS_COMPLETED);
            step.setActualEndTime(now);
            if (dto.getActualDuration() != null) {
                step.setActualDuration(dto.getActualDuration());
            } else if (step.getActualStartTime() != null) {
                // 自动计算耗时（分钟）
                long minutes = ChronoUnit.MINUTES.between(step.getActualStartTime(), now);
                step.setActualDuration((int) minutes);
            }
            if (dto.getExecutionRemark() != null) {
                step.setExecutionRemark(dto.getExecutionRemark());
            }
            stepMapper.updateById(step);
            recordLog(dto.getPlanId(), dto.getStepId(), ctx, CutoverConstant.ACTION_STEP_EXECUTE,
                    "完成步骤：" + step.getStepName(), CutoverConstant.LOG_LEVEL_INFO);
            // 检查是否所有步骤完成
            checkAllStepsCompleted(plan);
        } else {
            throw BusinessException.stateNotAllowed("当前步骤状态不允许执行操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackStep(CutoverStepExecuteDTO dto) {
        CutoverPlanEntity plan = requirePlan(dto.getPlanId());
        if (!CutoverConstant.PLAN_STATUS_EXECUTING.equals(plan.getStatus())) {
            throw BusinessException.stateNotAllowed("割接方案非执行中状态，无法回退步骤");
        }
        CutoverStepEntity step = requireStep(dto.getStepId(), dto.getPlanId());
        if (!CutoverConstant.STEP_STATUS_EXECUTING.equals(step.getStatus())) {
            throw BusinessException.stateNotAllowed("仅执行中状态步骤可回退");
        }
        UserContext ctx = requireUserContext();
        step.setStatus(CutoverConstant.STEP_STATUS_ROLLED_BACK);
        step.setActualEndTime(LocalDateTime.now());
        if (dto.getExecutionRemark() != null) {
            step.setExecutionRemark(dto.getExecutionRemark());
        }
        if (dto.getExceptionRemark() != null) {
            step.setExceptionRemark(dto.getExceptionRemark());
        }
        stepMapper.updateById(step);
        recordLog(dto.getPlanId(), dto.getStepId(), ctx, CutoverConstant.ACTION_STEP_ROLLBACK,
                "回退步骤：" + step.getStepName() + appendRemark(dto.getExceptionRemark()),
                CutoverConstant.LOG_LEVEL_WARN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exceptionStep(CutoverStepExecuteDTO dto) {
        CutoverPlanEntity plan = requirePlan(dto.getPlanId());
        if (!CutoverConstant.PLAN_STATUS_EXECUTING.equals(plan.getStatus())) {
            throw BusinessException.stateNotAllowed("割接方案非执行中状态，无法标记异常");
        }
        CutoverStepEntity step = requireStep(dto.getStepId(), dto.getPlanId());
        UserContext ctx = requireUserContext();
        step.setStatus(CutoverConstant.STEP_STATUS_ABORTED);
        step.setActualEndTime(LocalDateTime.now());
        if (dto.getExceptionRemark() != null) {
            step.setExceptionRemark(dto.getExceptionRemark());
        }
        stepMapper.updateById(step);
        recordLog(dto.getPlanId(), dto.getStepId(), ctx, CutoverConstant.ACTION_STEP_EXCEPTION,
                "步骤异常：" + step.getStepName() + appendRemark(dto.getExceptionRemark()),
                CutoverConstant.LOG_LEVEL_ERROR);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(CutoverCompleteDTO dto) {
        CutoverPlanEntity entity = requirePlan(dto.getPlanId());
        if (!CutoverConstant.PLAN_STATUS_EXECUTING.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅执行中状态可完成");
        }
        // 校验所有步骤已完成
        LambdaQueryWrapper<CutoverStepEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CutoverStepEntity::getPlanId, dto.getPlanId());
        wrapper.ne(CutoverStepEntity::getStatus, CutoverConstant.STEP_STATUS_COMPLETED);
        Long unfinished = stepMapper.selectCount(wrapper);
        if (unfinished != null && unfinished > 0) {
            throw BusinessException.stateNotAllowed("存在未完成的步骤，无法完成割接");
        }
        UserContext ctx = requireUserContext();
        entity.setStatus(CutoverConstant.PLAN_STATUS_COMPLETED);
        entity.setActualEndTime(LocalDateTime.now());
        entity.setSummary(dto.getSummary());
        entity.setProblemImprovement(dto.getProblemImprovement());
        planMapper.updateById(entity);
        recordLog(dto.getPlanId(), null, ctx, CutoverConstant.ACTION_COMPLETE,
                "割接完成" + (dto.getSummary() != null ? "，总结：" + dto.getSummary() : ""),
                CutoverConstant.LOG_LEVEL_INFO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abort(Long planId, String remark) {
        CutoverPlanEntity entity = requirePlan(planId);
        if (CutoverConstant.PLAN_STATUS_COMPLETED.equals(entity.getStatus())
                || CutoverConstant.PLAN_STATUS_ABORTED.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("已完成或已中止的方案不可再中止");
        }
        UserContext ctx = requireUserContext();
        entity.setStatus(CutoverConstant.PLAN_STATUS_ABORTED);
        if (entity.getActualStartTime() != null && entity.getActualEndTime() == null) {
            entity.setActualEndTime(LocalDateTime.now());
        }
        planMapper.updateById(entity);
        // 同时中止所有未完成步骤
        LambdaQueryWrapper<CutoverStepEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CutoverStepEntity::getPlanId, planId);
        wrapper.notIn(CutoverStepEntity::getStatus,
                CutoverConstant.STEP_STATUS_COMPLETED,
                CutoverConstant.STEP_STATUS_ROLLED_BACK,
                CutoverConstant.STEP_STATUS_ABORTED);
        List<CutoverStepEntity> pendingSteps = stepMapper.selectList(wrapper);
        for (CutoverStepEntity s : pendingSteps) {
            s.setStatus(CutoverConstant.STEP_STATUS_ABORTED);
            stepMapper.updateById(s);
        }
        recordLog(planId, null, ctx, CutoverConstant.ACTION_ABORT,
                "中止割接" + appendRemark(remark), CutoverConstant.LOG_LEVEL_ERROR);
    }

    /* ============ 查询 ============ */

    @Override
    public List<CutoverExecutionLogVO> listLogs(Long planId) {
        requirePlan(planId);
        return listLogVOs(planId);
    }

    /* ============ 私有方法 ============ */

    private CutoverPlanEntity requirePlan(Long id) {
        CutoverPlanEntity entity = planMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("割接方案");
        }
        return entity;
    }

    private CutoverStepEntity requireStep(Long stepId, Long planId) {
        CutoverStepEntity step = stepMapper.selectById(stepId);
        if (step == null || !planId.equals(step.getPlanId())) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "割接步骤不存在");
        }
        return step;
    }

    private UserContext requireUserContext() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }
        return ctx;
    }

    /**
     * 保存步骤列表（草稿创建/更新时使用）。
     *
     * @param planId    方案ID
     * @param steps     步骤列表
     * @param isCreate  是否新建（true=新增，false=先删后建）
     */
    private void saveSteps(Long planId, List<CutoverStepDTO> steps, boolean isCreate) {
        if (!isCreate) {
            // 删除原有步骤
            LambdaQueryWrapper<CutoverStepEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CutoverStepEntity::getPlanId, planId);
            stepMapper.delete(wrapper);
        }
        if (steps == null || steps.isEmpty()) {
            return;
        }
        int sortOrder = 1;
        for (CutoverStepDTO dto : steps) {
            CutoverStepEntity entity = new CutoverStepEntity();
            BeanUtils.copyProperties(dto, entity);
            entity.setId(null); // 新增
            entity.setPlanId(planId);
            entity.setStatus(CutoverConstant.STEP_STATUS_PENDING);
            // 自动校正序号
            if (dto.getSortOrder() == null) {
                entity.setSortOrder(sortOrder);
            } else {
                entity.setSortOrder(dto.getSortOrder());
            }
            sortOrder++;
            stepMapper.insert(entity);
        }
    }

    /**
     * 统计步骤总数与已完成数。
     *
     * @return int[]{总数, 已完成数}
     */
    private int[] countSteps(Long planId) {
        LambdaQueryWrapper<CutoverStepEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CutoverStepEntity::getPlanId, planId);
        Long total = stepMapper.selectCount(wrapper);
        wrapper.eq(CutoverStepEntity::getStatus, CutoverConstant.STEP_STATUS_COMPLETED);
        Long completed = stepMapper.selectCount(wrapper);
        return new int[]{
                total == null ? 0 : total.intValue(),
                completed == null ? 0 : completed.intValue()
        };
    }

    private List<CutoverStepVO> listStepVOs(Long planId) {
        LambdaQueryWrapper<CutoverStepEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CutoverStepEntity::getPlanId, planId);
        wrapper.orderByAsc(CutoverStepEntity::getSortOrder);
        List<CutoverStepEntity> entities = stepMapper.selectList(wrapper);
        List<CutoverStepVO> list = new ArrayList<>();
        for (CutoverStepEntity e : entities) {
            CutoverStepVO vo = new CutoverStepVO();
            BeanUtils.copyProperties(e, vo);
            list.add(vo);
        }
        return list;
    }

    private List<CutoverExecutionLogVO> listLogVOs(Long planId) {
        LambdaQueryWrapper<CutoverExecutionLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CutoverExecutionLogEntity::getPlanId, planId);
        wrapper.orderByAsc(CutoverExecutionLogEntity::getLogTime);
        List<CutoverExecutionLogEntity> entities = logMapper.selectList(wrapper);
        List<CutoverExecutionLogVO> list = new ArrayList<>();
        for (CutoverExecutionLogEntity e : entities) {
            CutoverExecutionLogVO vo = new CutoverExecutionLogVO();
            BeanUtils.copyProperties(e, vo);
            list.add(vo);
        }
        return list;
    }

    /**
     * 检查所有步骤是否已完成，是则不自动完成（需 PM 手动确认），仅记录日志。
     */
    private void checkAllStepsCompleted(CutoverPlanEntity plan) {
        int[] counts = countSteps(plan.getId());
        if (counts[0] > 0 && counts[0] == counts[1]) {
            log.info("[Cutover] 方案 {} 所有步骤已完成，等待 PM 提交总结完成", plan.getId());
        }
    }

    /**
     * 记录操作日志。
     */
    private void recordLog(Long planId, Long stepId, UserContext ctx,
                           String action, String content, String logLevel) {
        CutoverExecutionLogEntity logEntity = new CutoverExecutionLogEntity();
        logEntity.setPlanId(planId);
        logEntity.setStepId(stepId);
        if (ctx != null) {
            logEntity.setOperatorId(ctx.getUserId());
            logEntity.setOperatorName(
                    ctx.getRealName() != null ? ctx.getRealName() : ctx.getUserName());
        }
        logEntity.setAction(action);
        logEntity.setLogTime(LocalDateTime.now());
        logEntity.setLogContent(content);
        logEntity.setLogLevel(logLevel);
        logEntity.setCreateTime(LocalDateTime.now());
        logMapper.insert(logEntity);
    }

    private String appendRemark(String remark) {
        return (remark != null && !remark.isBlank()) ? "，意见：" + remark : "";
    }
}
