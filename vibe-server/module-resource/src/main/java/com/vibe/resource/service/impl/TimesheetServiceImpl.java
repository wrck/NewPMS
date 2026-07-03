package com.vibe.resource.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.resource.constant.ResourceConstant;
import com.vibe.resource.dto.BusinessTripDTO;
import com.vibe.resource.dto.BusinessTripQueryDTO;
import com.vibe.resource.dto.TimesheetApprovalDTO;
import com.vibe.resource.dto.TimesheetDTO;
import com.vibe.resource.dto.TimesheetQueryDTO;
import com.vibe.resource.dto.TimesheetStatsQueryDTO;
import com.vibe.resource.entity.BusinessTripEntity;
import com.vibe.resource.entity.EngineerEntity;
import com.vibe.resource.entity.TimesheetEntity;
import com.vibe.resource.mapper.BusinessTripMapper;
import com.vibe.resource.mapper.EngineerMapper;
import com.vibe.resource.mapper.TimesheetMapper;
import com.vibe.resource.service.TimesheetService;
import com.vibe.resource.vo.BusinessTripVO;
import com.vibe.resource.vo.TimesheetStatsVO;
import com.vibe.resource.vo.TimesheetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工时服务实现（含出差/加班统计）
 *
 * <p>状态流转：</p>
 * <ul>
 *   <li>工时：SUBMITTED → APPROVED / REJECTED（PM 审批）</li>
 *   <li>出差：PENDING → APPROVED → COMPLETED / REJECTED</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimesheetServiceImpl implements TimesheetService {

    private final TimesheetMapper timesheetMapper;
    private final BusinessTripMapper businessTripMapper;
    private final EngineerMapper engineerMapper;

    /** 标准工时：8 小时/天，用于人天折算 */
    private static final BigDecimal STANDARD_HOURS_PER_DAY = BigDecimal.valueOf(8);

    @Override
    public PageResult<TimesheetVO> page(TimesheetQueryDTO query) {
        IPage<TimesheetVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<TimesheetVO> result = timesheetMapper.selectTimesheetPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public TimesheetVO getDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工时ID不能为空");
        }
        TimesheetVO vo = timesheetMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.notFound("工时");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TimesheetDTO dto) {
        TimesheetEntity entity = new TimesheetEntity();
        // 工程师ID：优先取 DTO，未填则按当前登录用户解析
        Long engineerId = resolveEngineerId(dto.getEngineerId());
        entity.setEngineerId(engineerId);
        entity.setProjectId(dto.getProjectId());
        entity.setTaskId(dto.getTaskId());
        entity.setWorkDate(dto.getWorkDate());
        entity.setHours(dto.getHours());
        entity.setOvertimeHours(dto.getOvertimeHours() == null
                ? BigDecimal.ZERO : dto.getOvertimeHours());
        entity.setTravelDays(dto.getTravelDays() == null ? 0 : dto.getTravelDays());
        entity.setDescription(dto.getDescription());
        entity.setStatus(ResourceConstant.TIMESHEET_SUBMITTED);
        timesheetMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TimesheetDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工时ID不能为空");
        }
        TimesheetEntity exist = timesheetMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.notFound("工时");
        }
        // 已审批的工时不可编辑
        if (ResourceConstant.TIMESHEET_APPROVED.equals(exist.getStatus())
                || ResourceConstant.TIMESHEET_REJECTED.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("已审批的工时不可编辑");
        }
        if (dto.getEngineerId() != null) {
            exist.setEngineerId(dto.getEngineerId());
        }
        exist.setProjectId(dto.getProjectId());
        exist.setTaskId(dto.getTaskId());
        exist.setWorkDate(dto.getWorkDate());
        exist.setHours(dto.getHours());
        exist.setOvertimeHours(dto.getOvertimeHours() == null
                ? BigDecimal.ZERO : dto.getOvertimeHours());
        exist.setTravelDays(dto.getTravelDays() == null ? 0 : dto.getTravelDays());
        exist.setDescription(dto.getDescription());
        // 编辑后回到待审批
        exist.setStatus(ResourceConstant.TIMESHEET_SUBMITTED);
        timesheetMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工时ID不能为空");
        }
        timesheetMapper.deleteById(id);
    }

    /**
     * PM 审批工时：SUBMITTED → APPROVED / REJECTED。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(TimesheetApprovalDTO dto) {
        if (CollectionUtils.isEmpty(dto.getIds())) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "工时ID列表不能为空");
        }
        if (!ResourceConstant.TIMESHEET_APPROVED.equals(dto.getDecision())
                && !ResourceConstant.TIMESHEET_REJECTED.equals(dto.getDecision())) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                "审批结果非法，仅支持 APPROVED / REJECTED");
        }
        Long approverId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        for (Long id : dto.getIds()) {
            TimesheetEntity exist = timesheetMapper.selectById(id);
            if (exist == null) {
                log.warn("[工时审批] 工时ID={} 不存在，跳过", id);
                continue;
            }
            if (!ResourceConstant.TIMESHEET_SUBMITTED.equals(exist.getStatus())) {
                log.warn("[工时审批] 工时ID={} 状态={} 非待审批，跳过", id, exist.getStatus());
                continue;
            }
            exist.setStatus(dto.getDecision());
            exist.setApproverId(approverId);
            exist.setApproveTime(now);
            timesheetMapper.updateById(exist);
        }
    }

    @Override
    public List<TimesheetStatsVO> stats(TimesheetStatsQueryDTO query) {
        if (StringUtils.hasText(query.getDimension())) {
            // 规范化 dimension，未识别时按 ENGINEER 兜底
            String dim = query.getDimension();
            if (!"ENGINEER".equals(dim) && !"PROJECT".equals(dim) && !"MONTHLY".equals(dim)) {
                query.setDimension("ENGINEER");
            }
        } else {
            query.setDimension("ENGINEER");
        }
        return timesheetMapper.selectStats(query);
    }

    @Override
    public TimesheetStatsVO summaryStats(Long engineerId, LocalDate startDate, LocalDate endDate) {
        // 直接复用 selectStats：按工程师维度 + 时间范围
        TimesheetStatsQueryDTO query = new TimesheetStatsQueryDTO();
        query.setDimension("ENGINEER");
        query.setEngineerId(engineerId);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        List<TimesheetStatsVO> list = timesheetMapper.selectStats(query);
        if (CollectionUtils.isEmpty(list)) {
            TimesheetStatsVO empty = new TimesheetStatsVO();
            empty.setEngineerId(engineerId);
            empty.setTotalHours(BigDecimal.ZERO);
            empty.setTotalOvertimeHours(BigDecimal.ZERO);
            empty.setTotalTravelDays(0);
            empty.setTotalManDays(BigDecimal.ZERO);
            empty.setRecordCount(0);
            return empty;
        }
        // 单工程师汇总：若有多条（理论只会有一条），累加返回
        TimesheetStatsVO target = list.get(0);
        if (list.size() > 1) {
            BigDecimal totalHours = BigDecimal.ZERO;
            BigDecimal totalOvertime = BigDecimal.ZERO;
            int totalTravel = 0;
            int records = 0;
            for (TimesheetStatsVO vo : list) {
                totalHours = totalHours.add(vo.getTotalHours() == null
                        ? BigDecimal.ZERO : vo.getTotalHours());
                totalOvertime = totalOvertime.add(vo.getTotalOvertimeHours() == null
                        ? BigDecimal.ZERO : vo.getTotalOvertimeHours());
                totalTravel += vo.getTotalTravelDays() == null ? 0 : vo.getTotalTravelDays();
                records += vo.getRecordCount() == null ? 0 : vo.getRecordCount();
            }
            target.setTotalHours(totalHours);
            target.setTotalOvertimeHours(totalOvertime);
            target.setTotalTravelDays(totalTravel);
            target.setTotalManDays(totalHours.divide(STANDARD_HOURS_PER_DAY, 2, RoundingMode.HALF_UP));
            target.setRecordCount(records);
        }
        return target;
    }

    /* ============ 出差管理 ============ */

    @Override
    public PageResult<BusinessTripVO> tripPage(BusinessTripQueryDTO query) {
        IPage<BusinessTripVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<BusinessTripVO> result = businessTripMapper.selectTripPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public BusinessTripVO getTripDetail(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "出差ID不能为空");
        }
        BusinessTripVO vo = businessTripMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.notFound("出差记录");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTrip(BusinessTripDTO dto) {
        if (dto.getStartDate() == null || dto.getEndDate() == null
                || dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "出差日期范围非法");
        }
        BusinessTripEntity entity = new BusinessTripEntity();
        entity.setEngineerId(dto.getEngineerId());
        entity.setProjectId(dto.getProjectId());
        entity.setTaskId(dto.getTaskId());
        entity.setOrigin(dto.getOrigin());
        entity.setDestination(dto.getDestination());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setTransportMode(StringUtils.hasText(dto.getTransportMode())
                ? dto.getTransportMode() : ResourceConstant.TRANSPORT_OTHER);
        entity.setAccommodation(dto.getAccommodation());
        entity.setEstimatedCost(dto.getEstimatedCost());
        entity.setReason(dto.getReason());
        entity.setRemark(dto.getRemark());
        entity.setStatus(ResourceConstant.APPROVAL_PENDING);
        businessTripMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrip(BusinessTripDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "出差ID不能为空");
        }
        BusinessTripEntity exist = businessTripMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.notFound("出差记录");
        }
        if (!ResourceConstant.APPROVAL_PENDING.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("非待审批状态的出差不可编辑");
        }
        exist.setEngineerId(dto.getEngineerId());
        exist.setProjectId(dto.getProjectId());
        exist.setTaskId(dto.getTaskId());
        exist.setOrigin(dto.getOrigin());
        exist.setDestination(dto.getDestination());
        exist.setStartDate(dto.getStartDate());
        exist.setEndDate(dto.getEndDate());
        if (StringUtils.hasText(dto.getTransportMode())) {
            exist.setTransportMode(dto.getTransportMode());
        }
        exist.setAccommodation(dto.getAccommodation());
        exist.setEstimatedCost(dto.getEstimatedCost());
        exist.setReason(dto.getReason());
        exist.setRemark(dto.getRemark());
        businessTripMapper.updateById(exist);
    }

    /**
     * 出差审批：PENDING → APPROVED / REJECTED；APPROVED 可流转为 COMPLETED。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTrip(Long id, String decision) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "出差ID不能为空");
        }
        BusinessTripEntity exist = businessTripMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.notFound("出差记录");
        }
        Long approverId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        // COMPLETED 流转：仅 APPROVED 状态可流转
        if (ResourceConstant.TRIP_COMPLETED.equals(decision)) {
            if (!ResourceConstant.APPROVAL_APPROVED.equals(exist.getStatus())) {
                throw BusinessException.stateNotAllowed("仅已批准的出差可标记为已完成");
            }
            exist.setStatus(ResourceConstant.TRIP_COMPLETED);
            businessTripMapper.updateById(exist);
            return;
        }

        if (!ResourceConstant.APPROVAL_APPROVED.equals(decision)
                && !ResourceConstant.APPROVAL_REJECTED.equals(decision)) {
            throw BusinessException.of(ResultCode.PARAM_INVALID,
                "审批结果非法，仅支持 APPROVED / REJECTED / COMPLETED");
        }
        if (!ResourceConstant.APPROVAL_PENDING.equals(exist.getStatus())) {
            throw BusinessException.stateNotAllowed("当前出差状态不允许审批");
        }
        exist.setStatus(decision);
        exist.setApproverId(approverId);
        exist.setApproveTime(now);
        businessTripMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrip(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "出差ID不能为空");
        }
        businessTripMapper.deleteById(id);
    }

    /* ============ 私有方法 ============ */

    /**
     * 解析工程师ID：DTO 显式传入优先；未传则按当前登录用户查询工程师档案。
     */
    private Long resolveEngineerId(Long engineerId) {
        if (engineerId != null) {
            return engineerId;
        }
        Long userId = currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法识别当前登录用户");
        }
        EngineerEntity engineer = engineerMapper.selectByUserId(userId);
        if (engineer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND,
                "当前登录用户未关联工程师档案");
        }
        return engineer.getId();
    }

    private Long currentUserId() {
        UserContext ctx = UserContextHolder.get();
        return ctx == null ? null : ctx.getUserId();
    }
}
