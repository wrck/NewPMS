package com.vibe.resource.service;

import com.vibe.common.result.PageResult;
import com.vibe.resource.dto.BusinessTripDTO;
import com.vibe.resource.dto.BusinessTripQueryDTO;
import com.vibe.resource.dto.TimesheetApprovalDTO;
import com.vibe.resource.dto.TimesheetDTO;
import com.vibe.resource.dto.TimesheetQueryDTO;
import com.vibe.resource.dto.TimesheetStatsQueryDTO;
import com.vibe.resource.vo.BusinessTripVO;
import com.vibe.resource.vo.TimesheetStatsVO;
import com.vibe.resource.vo.TimesheetVO;

import java.util.List;

/**
 * 工时服务（含出差/加班统计）
 *
 * @author vibe
 */
public interface TimesheetService {

    /**
     * 分页查询工时
     */
    PageResult<TimesheetVO> page(TimesheetQueryDTO query);

    /**
     * 工时详情
     */
    TimesheetVO getDetail(Long id);

    /**
     * 工时填报
     */
    Long create(TimesheetDTO dto);

    /**
     * 编辑工时
     */
    void update(TimesheetDTO dto);

    /**
     * 删除工时
     */
    void delete(Long id);

    /**
     * PM 审批工时（SUBMITTED → APPROVED/REJECTED）
     */
    void approve(TimesheetApprovalDTO dto);

    /**
     * 人天统计多维查询（按工程师/项目/月度）
     */
    List<TimesheetStatsVO> stats(TimesheetStatsQueryDTO query);

    /**
     * 出差/加班统计：按工程师 + 时间范围汇总出差天数与加班时长
     */
    TimesheetStatsVO summaryStats(Long engineerId,
                                  java.time.LocalDate startDate,
                                  java.time.LocalDate endDate);

    /* ============ 出差管理 ============ */

    /**
     * 分页查询出差记录
     */
    PageResult<BusinessTripVO> tripPage(BusinessTripQueryDTO query);

    /**
     * 出差详情
     */
    BusinessTripVO getTripDetail(Long id);

    /**
     * 出差申请
     */
    Long createTrip(BusinessTripDTO dto);

    /**
     * 编辑出差
     */
    void updateTrip(BusinessTripDTO dto);

    /**
     * 出差审批（PENDING → APPROVED/REJECTED；APPROVED 后可流转为 COMPLETED）
     */
    void approveTrip(Long id, String decision);

    /**
     * 删除出差
     */
    void deleteTrip(Long id);
}
