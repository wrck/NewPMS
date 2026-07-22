package com.vibe.acceptance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.acceptance.constant.AcceptanceConstant;
import com.vibe.acceptance.dto.AcceptanceTaskActionDTO;
import com.vibe.acceptance.dto.AcceptanceTaskCreateDTO;
import com.vibe.acceptance.dto.AcceptanceTaskQueryDTO;
import com.vibe.acceptance.entity.AcceptanceTaskEntity;
import com.vibe.acceptance.entity.AcceptanceTestRecordEntity;
import com.vibe.acceptance.mapper.AcceptanceTaskMapper;
import com.vibe.acceptance.mapper.AcceptanceTestRecordMapper;
import com.vibe.acceptance.service.AcceptanceTaskService;
import com.vibe.acceptance.vo.AcceptanceTaskVO;
import com.vibe.acceptance.vo.AcceptanceTestRecordVO;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.event.DomainEventPublisher;
import com.vibe.event.events.AcceptancePassedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 验收任务 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class AcceptanceTaskServiceImpl implements AcceptanceTaskService {

    private final AcceptanceTaskMapper taskMapper;
    private final AcceptanceTestRecordMapper testRecordMapper;

    @Override
    public PageResult<AcceptanceTaskVO> page(AcceptanceTaskQueryDTO query) {
        IPage<AcceptanceTaskVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<AcceptanceTaskVO> result = taskMapper.selectTaskPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AcceptanceTaskVO getDetail(Long id) {
        AcceptanceTaskVO vo = taskMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.notFound("验收任务");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AcceptanceTaskCreateDTO dto) {
        AcceptanceTaskEntity entity = new AcceptanceTaskEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(AcceptanceConstant.TASK_STATUS_DRAFT);
        taskMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AcceptanceTaskCreateDTO dto) {
        AcceptanceTaskEntity entity = taskMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("验收任务");
        }
        if (!AcceptanceConstant.TASK_STATUS_DRAFT.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的验收任务可修改");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        taskMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AcceptanceTaskEntity entity = taskMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("验收任务");
        }
        if (!AcceptanceConstant.TASK_STATUS_DRAFT.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的验收任务可删除");
        }
        taskMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(AcceptanceTaskActionDTO dto) {
        AcceptanceTaskEntity entity = taskMapper.selectById(dto.getTaskId());
        if (entity == null) {
            throw BusinessException.notFound("验收任务");
        }
        if (!AcceptanceConstant.TASK_STATUS_DRAFT.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅草稿状态的验收任务可申请");
        }
        entity.setStatus(AcceptanceConstant.TASK_STATUS_APPLIED);
        entity.setApplyUserId(UserContextHolder.getUserId());
        entity.setApplyTime(LocalDateTime.now());
        if (dto.getRemark() != null) {
            entity.setRemark(dto.getRemark());
        }
        taskMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void internalAudit(AcceptanceTaskActionDTO dto) {
        AcceptanceTaskEntity entity = taskMapper.selectById(dto.getTaskId());
        if (entity == null) {
            throw BusinessException.notFound("验收任务");
        }
        if (!AcceptanceConstant.TASK_STATUS_APPLIED.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅已申请状态的验收任务可内部审核");
        }
        String result = dto.getResult();
        if ("PASS".equals(result)) {
            entity.setStatus(AcceptanceConstant.TASK_STATUS_INTERNAL_AUDITED);
            entity.setInternalAuditResult("PASS");
        } else if ("REJECT".equals(result)) {
            entity.setStatus(AcceptanceConstant.TASK_STATUS_REJECTED);
            entity.setInternalAuditResult("REJECT");
        } else {
            throw BusinessException.of(400, "审核结果必须为 PASS 或 REJECT");
        }
        entity.setInternalAuditUserId(UserContextHolder.getUserId());
        entity.setInternalAuditTime(LocalDateTime.now());
        taskMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startCustomerSign(Long taskId) {
        AcceptanceTaskEntity entity = taskMapper.selectById(taskId);
        if (entity == null) {
            throw BusinessException.notFound("验收任务");
        }
        if (!AcceptanceConstant.TASK_STATUS_INTERNAL_AUDITED.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅内部审核通过的验收任务可发起客户签核");
        }
        entity.setStatus(AcceptanceConstant.TASK_STATUS_CUSTOMER_SIGNING);
        // 生成签核链接 token（简单实现：使用任务ID+时间戳）
        entity.setCustomerSignLink("acceptance-sign-" + taskId + "-" + System.currentTimeMillis());
        taskMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void customerSign(AcceptanceTaskActionDTO dto) {
        AcceptanceTaskEntity entity = taskMapper.selectById(dto.getTaskId());
        if (entity == null) {
            throw BusinessException.notFound("验收任务");
        }
        if (!AcceptanceConstant.TASK_STATUS_CUSTOMER_SIGNING.equals(entity.getStatus())) {
            throw BusinessException.stateNotAllowed("仅客户签核中的验收任务可签核");
        }
        String result = dto.getResult();
        if ("PASS".equals(result) || "CONDITIONAL_PASS".equals(result)) {
            entity.setStatus(AcceptanceConstant.TASK_STATUS_COMPLETED);
            entity.setCustomerSignResult(result);
        } else if ("REJECT".equals(result)) {
            entity.setStatus(AcceptanceConstant.TASK_STATUS_REJECTED);
            entity.setCustomerSignResult("REJECT");
        } else {
            throw BusinessException.of(400, "签核结果必须为 PASS / CONDITIONAL_PASS / REJECT");
        }
        entity.setCustomerSignUser(dto.getCustomerSignUser());
        entity.setCustomerSignTime(LocalDateTime.now());
        taskMapper.updateById(entity);
    }

    @Override
    public List<AcceptanceTestRecordVO> listTestRecords(Long taskId) {
        LambdaQueryWrapper<AcceptanceTestRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AcceptanceTestRecordEntity::getTaskId, taskId)
               .orderByAsc(AcceptanceTestRecordEntity::getTestTime);
        List<AcceptanceTestRecordEntity> list = testRecordMapper.selectList(wrapper);
        List<AcceptanceTestRecordVO> result = new ArrayList<>(list.size());
        for (AcceptanceTestRecordEntity e : list) {
            AcceptanceTestRecordVO vo = new AcceptanceTestRecordVO();
            BeanUtils.copyProperties(e, vo);
            result.add(vo);
        }
        return result;
    }
}
