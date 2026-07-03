package com.vibe.resource.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.resource.constant.ResourceConstant;
import com.vibe.resource.dto.EngineerLeaveDTO;
import com.vibe.resource.dto.EngineerLeaveQueryDTO;
import com.vibe.resource.dto.EngineerScheduleDTO;
import com.vibe.resource.dto.EngineerScheduleQueryDTO;
import com.vibe.resource.service.EngineerScheduleService;
import com.vibe.resource.vo.ConflictDetectVO;
import com.vibe.resource.vo.EngineerLeaveVO;
import com.vibe.resource.vo.EngineerScheduleVO;
import com.vibe.resource.vo.WorkloadHeatmapVO;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工程师排期 Controller
 *
 * <p>路径前缀 {@code /api/v1/schedules}。提供排期管理、日历视图、冲突检测、
 * 负荷热力图与请假/培训时间块管理。</p>
 *
 * @author vibe
 */
@Tag(name = "排期管理", description = "排期 CRUD、日历视图、冲突检测、负荷热力图、请假管理")
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class EngineerScheduleController {

    private final EngineerScheduleService scheduleService;

    @Operation(summary = "分页查询排期")
    @OperationLog(module = "排期管理", type = ResourceConstant.BIZ_QUERY, description = "分页查询排期")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping
    public Result<PageResult<EngineerScheduleVO>> page(@ParameterObject EngineerScheduleQueryDTO query) {
        return Result.success(scheduleService.page(query));
    }

    @Operation(summary = "日历视图查询（按工程师/任务/时间范围）")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping("/calendar")
    public Result<List<EngineerScheduleVO>> calendar(
            @Parameter(description = "工程师ID") @RequestParam(required = false) Long engineerId,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "开始时间") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "排期类型") @RequestParam(required = false) String scheduleType) {
        return Result.success(scheduleService.calendar(engineerId, taskId, startTime, endTime, scheduleType));
    }

    @Operation(summary = "新增排期（含冲突检测，紧急调配可置 ignoreConflict=true）")
    @OperationLog(module = "排期管理", type = ResourceConstant.BIZ_INSERT,
            description = "新增排期", saveResponse = true)
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody EngineerScheduleDTO dto) {
        return Result.success(scheduleService.createSchedule(dto));
    }

    @Operation(summary = "编辑排期")
    @OperationLog(module = "排期管理", type = ResourceConstant.BIZ_UPDATE, description = "编辑排期")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EngineerScheduleDTO dto) {
        dto.setId(id);
        scheduleService.updateSchedule(dto);
        return Result.success();
    }

    @Operation(summary = "删除排期")
    @OperationLog(module = "排期管理", type = ResourceConstant.BIZ_DELETE, description = "删除排期")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return Result.success();
    }

    @Operation(summary = "冲突检测：返回是否冲突及冲突排期列表")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @GetMapping("/conflict")
    public Result<ConflictDetectVO> detectConflict(
            @Parameter(description = "工程师ID", required = true) @RequestParam Long engineerId,
            @Parameter(description = "待检测开始时间", required = true) @RequestParam
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "待检测结束时间", required = true) @RequestParam
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "排除的排期ID（编辑场景）") @RequestParam(required = false) Long excludeId) {
        return Result.success(scheduleService.detectConflict(engineerId, startTime, endTime, excludeId));
    }

    @Operation(summary = "负荷热力图数据（各工程师某时段任务数 + 负荷等级）")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @GetMapping("/workload-heatmap")
    public Result<List<WorkloadHeatmapVO>> workloadHeatmap(
            @Parameter(description = "开始时间", required = true) @RequestParam
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true) @RequestParam
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "区域") @RequestParam(required = false) String region,
            @Parameter(description = "工程师ID列表（逗号分隔）") @RequestParam(required = false) String engineerIds) {
        List<Long> ids = engineerIds == null || engineerIds.isBlank()
                ? null : List.of(engineerIds.split(",")).stream().map(Long::valueOf).toList();
        return Result.success(scheduleService.workloadHeatmap(startTime, endTime, region, ids));
    }

    /* ============ 请假/培训时间块管理 ============ */

    @Operation(summary = "分页查询请假记录")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @GetMapping("/leaves")
    public Result<PageResult<EngineerLeaveVO>> leavePage(@ParameterObject EngineerLeaveQueryDTO query) {
        return Result.success(scheduleService.leavePage(query));
    }

    @Operation(summary = "新增请假")
    @OperationLog(module = "排期管理", type = ResourceConstant.BIZ_INSERT,
            description = "新增请假", saveResponse = true)
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @PostMapping("/leaves")
    public Result<Long> createLeave(@Valid @RequestBody EngineerLeaveDTO dto) {
        return Result.success(scheduleService.createLeave(dto));
    }

    @Operation(summary = "编辑请假")
    @OperationLog(module = "排期管理", type = ResourceConstant.BIZ_UPDATE, description = "编辑请假")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_ENGINEER + "')")
    @PutMapping("/leaves/{id}")
    public Result<Void> updateLeave(@PathVariable Long id, @Valid @RequestBody EngineerLeaveDTO dto) {
        dto.setId(id);
        scheduleService.updateLeave(dto);
        return Result.success();
    }

    @Operation(summary = "审批请假（APPROVED 时同步写入排期 LEAVE 块）")
    @OperationLog(module = "排期管理", type = ResourceConstant.BIZ_APPROVE, description = "审批请假")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @PutMapping("/leaves/{id}/approve")
    public Result<Void> approveLeave(@PathVariable Long id,
                                     @Parameter(description = "审批结果 APPROVED/REJECTED", required = true)
                                     @RequestParam String decision) {
        scheduleService.approveLeave(id, decision);
        return Result.success();
    }

    @Operation(summary = "删除请假")
    @OperationLog(module = "排期管理", type = ResourceConstant.BIZ_DELETE, description = "删除请假")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @DeleteMapping("/leaves/{id}")
    public Result<Void> deleteLeave(@PathVariable Long id) {
        scheduleService.deleteLeave(id);
        return Result.success();
    }
}
