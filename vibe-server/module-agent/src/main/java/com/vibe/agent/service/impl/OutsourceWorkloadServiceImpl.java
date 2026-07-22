package com.vibe.agent.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.OutsourceWorkloadDTO;
import com.vibe.agent.dto.OutsourceWorkloadQueryDTO;
import com.vibe.agent.entity.OutsourceWorkloadEntity;
import com.vibe.agent.mapper.OutsourceWorkloadMapper;
import com.vibe.agent.service.OutsourceWorkloadService;
import com.vibe.agent.vo.OutsourceWorkloadVO;
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

import java.util.List;

/**
 * 代理商工作量服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutsourceWorkloadServiceImpl implements OutsourceWorkloadService {

    private final OutsourceWorkloadMapper workloadMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(OutsourceWorkloadDTO dto) {
        if (dto.getOutsourceTaskId() == null) {
            throw BusinessException.of(ResultCode.PARAM_MISSING, "转包任务ID不能为空");
        }
        OutsourceWorkloadEntity entity = new OutsourceWorkloadEntity();
        entity.setOutsourceTaskId(dto.getOutsourceTaskId());
        entity.setManDays(dto.getManDays());
        entity.setSiteCount(dto.getSiteCount());
        entity.setDeviceCount(dto.getDeviceCount());
        entity.setSubmittedBy(UserContextHolder.getUserId());
        entity.setStatus(AgentConstant.WORKLOAD_SUBMITTED);
        entity.setRemark(dto.getRemark());
        workloadMapper.insert(entity);
        log.info("代理商提交工作量: taskId={}, id={}", dto.getOutsourceTaskId(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        OutsourceWorkloadEntity exist = workloadMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "工作量记录不存在");
        }
        if (!AgentConstant.WORKLOAD_SUBMITTED.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("仅待确认状态的工作量可确认");
        }
        OutsourceWorkloadEntity update = new OutsourceWorkloadEntity();
        update.setId(id);
        update.setStatus(AgentConstant.WORKLOAD_CONFIRMED);
        update.setConfirmedBy(UserContextHolder.getUserId());
        workloadMapper.updateById(update);
        log.info("PM 确认工作量: id={}, confirmedBy={}", id, update.getConfirmedBy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String remark) {
        OutsourceWorkloadEntity exist = workloadMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "工作量记录不存在");
        }
        if (!AgentConstant.WORKLOAD_SUBMITTED.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("仅待确认状态的工作量可驳回");
        }
        OutsourceWorkloadEntity update = new OutsourceWorkloadEntity();
        update.setId(id);
        update.setStatus(AgentConstant.WORKLOAD_REJECTED);
        update.setConfirmedBy(UserContextHolder.getUserId());
        if (StringUtils.hasText(remark)) {
            update.setRemark(remark);
        }
        workloadMapper.updateById(update);
        log.info("PM 驳回工作量: id={}, remark={}", id, remark);
    }

    @Override
    public List<OutsourceWorkloadVO> listByTaskId(Long taskId) {
        if (taskId == null) {
            return List.of();
        }
        return workloadMapper.selectByTaskId(taskId);
    }

    @Override
    public PageResult<OutsourceWorkloadVO> page(OutsourceWorkloadQueryDTO query) {
        // AGENT_ADMIN 数据隔离：强制仅查看本公司工作量（覆盖前端传入的 agentCompanyId）
        UserContext ctx = UserContextHolder.get();
        if (ctx != null && ctx.hasRole(AgentConstant.ROLE_AGENT_ADMIN)) {
            Long tenantId = ctx.getTenantId();
            if (tenantId == null) {
                return PageResult.of(List.of(), 0L, query.getPage(), query.getSize());
            }
            query.setAgentCompanyId(tenantId);
        }

        IPage<OutsourceWorkloadVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<OutsourceWorkloadVO> result = workloadMapper.selectWorkloadPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
}
