package com.vibe.resource.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.resource.constant.ResourceConstant;
import com.vibe.resource.dto.BusinessTripDTO;
import com.vibe.resource.dto.BusinessTripQueryDTO;
import com.vibe.resource.dto.TimesheetApprovalDTO;
import com.vibe.resource.dto.TimesheetDTO;
import com.vibe.resource.dto.TimesheetQueryDTO;
import com.vibe.resource.dto.TimesheetStatsQueryDTO;
import com.vibe.resource.service.TimesheetService;
import com.vibe.resource.vo.BusinessTripVO;
import com.vibe.resource.vo.TimesheetStatsVO;
import com.vibe.resource.vo.TimesheetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 工时管理 Controller
 *
 * <p>路径前缀 {@code /api/v1/timesheets}。提供工时填报、PM 审批、人天统计多维查询，
 * 以及出差申请/审批管理。工时查询默认按 ENGINEER 角色过滤本人数据。</p>
 *
 * @author vibe
 */
@Tag(name = "工时管理", description = "工时填报/审批/统计、出差管理")
@RestController
@RequestMapping("/api/v1/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    @Operation(summary = "分页查询工时")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_QUERY, description = "分页查询工时")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping
    public Result<PageResult<TimesheetVO>> page(@ParameterObject TimesheetQueryDTO query) {
        return Result.success(timesheetService.page(query));
    }

    @Operation(summary = "工时详情")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping("/{id}")
    public Result<TimesheetVO> detail(@PathVariable Long id) {
        return Result.success(timesheetService.getDetail(id));
    }

    @Operation(summary = "工时填报")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_INSERT,
            description = "工时填报", saveResponse = true)
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TimesheetDTO dto) {
        return Result.success(timesheetService.create(dto));
    }

    @Operation(summary = "编辑工时")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_UPDATE, description = "编辑工时")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TimesheetDTO dto) {
        dto.setId(id);
        timesheetService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除工时")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_DELETE, description = "删除工时")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        timesheetService.delete(id);
        return Result.success();
    }

    @Operation(summary = "PM 审批工时（SUBMITTED → APPROVED/REJECTED）")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_APPROVE, description = "审批工时")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @PutMapping("/approve")
    public Result<Void> approve(@Valid @RequestBody TimesheetApprovalDTO dto) {
        timesheetService.approve(dto);
        return Result.success();
    }

    @Operation(summary = "人天统计多维查询（按工程师/项目/月度）")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @GetMapping("/stats")
    public Result<List<TimesheetStatsVO>> stats(@ParameterObject TimesheetStatsQueryDTO query) {
        return Result.success(timesheetService.stats(query));
    }

    @Operation(summary = "出差/加班统计：按工程师 + 时间范围汇总")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping("/summary")
    public Result<TimesheetStatsVO> summaryStats(
            @Parameter(description = "工程师ID") @RequestParam(required = false) Long engineerId,
            @Parameter(description = "开始日期") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(timesheetService.summaryStats(engineerId, startDate, endDate));
    }

    /* ============ 出差管理 ============ */

    @Operation(summary = "分页查询出差记录")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping("/trips")
    public Result<PageResult<BusinessTripVO>> tripPage(@ParameterObject BusinessTripQueryDTO query) {
        return Result.success(timesheetService.tripPage(query));
    }

    @Operation(summary = "出差详情")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping("/trips/{id}")
    public Result<BusinessTripVO> tripDetail(@PathVariable Long id) {
        return Result.success(timesheetService.getTripDetail(id));
    }

    @Operation(summary = "出差申请")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_INSERT,
            description = "出差申请", saveResponse = true)
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @PostMapping("/trips")
    public Result<Long> createTrip(@Valid @RequestBody BusinessTripDTO dto) {
        return Result.success(timesheetService.createTrip(dto));
    }

    @Operation(summary = "编辑出差")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_UPDATE, description = "编辑出差")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @PutMapping("/trips/{id}")
    public Result<Void> updateTrip(@PathVariable Long id, @Valid @RequestBody BusinessTripDTO dto) {
        dto.setId(id);
        timesheetService.updateTrip(dto);
        return Result.success();
    }

    @Operation(summary = "出差审批（PENDING → APPROVED/REJECTED；APPROVED 后可流转为 COMPLETED）")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_APPROVE, description = "出差审批")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @PutMapping("/trips/{id}/approve")
    public Result<Void> approveTrip(@PathVariable Long id,
                                    @Parameter(description = "审批结果 APPROVED/REJECTED/COMPLETED", required = true)
                                    @RequestParam String decision) {
        timesheetService.approveTrip(id, decision);
        return Result.success();
    }

    @Operation(summary = "删除出差")
    @OperationLog(module = "工时管理", type = ResourceConstant.BIZ_DELETE, description = "删除出差")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @DeleteMapping("/trips/{id}")
    public Result<Void> deleteTrip(@PathVariable Long id) {
        timesheetService.deleteTrip(id);
        return Result.success();
    }
}
