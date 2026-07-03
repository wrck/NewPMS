package com.vibe.agent.controller;

import com.vibe.agent.constant.AgentConstant;
import com.vibe.agent.dto.OutsourceWorkloadDTO;
import com.vibe.agent.service.OutsourceWorkloadService;
import com.vibe.agent.vo.OutsourceWorkloadVO;
import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 代理商工作量管理 Controller
 *
 * <p>路径：{@code /api/v1/outsource-tasks/{taskId}/workload}</p>
 *
 * <p><b>权限分工：</b></p>
 * <ul>
 *   <li>AGENT_ADMIN / AGENT_ENGINEER：提交工作量</li>
 *   <li>PM / SUPER_ADMIN：确认/驳回工作量</li>
 * </ul>
 *
 * @author vibe
 */
@Tag(name = "代理商工作量管理", description = "工作量提交/确认/驳回/查询")
@RestController
@RequestMapping("/api/v1/outsource-tasks/{taskId}/workload")
@RequiredArgsConstructor
public class OutsourceWorkloadController {

    private final OutsourceWorkloadService workloadService;

    @Operation(summary = "代理商提交工作量（人天/站点数/设备台数）")
    @OperationLog(module = AgentConstant.MODULE_WORKLOAD, type = "INSERT", description = "代理商提交工作量")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AGENT_ADMIN','AGENT_ENGINEER')")
    @PostMapping
    public Result<Long> submit(@PathVariable Long taskId,
                               @Valid @RequestBody OutsourceWorkloadDTO dto) {
        dto.setOutsourceTaskId(taskId);
        return Result.success(workloadService.submit(dto));
    }

    @Operation(summary = "查询工作量记录列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','AGENT_ADMIN')")
    @GetMapping
    public Result<List<OutsourceWorkloadVO>> list(@PathVariable Long taskId) {
        return Result.success(workloadService.listByTaskId(taskId));
    }

    @Operation(summary = "PM 确认工作量")
    @OperationLog(module = AgentConstant.MODULE_WORKLOAD, type = "UPDATE", description = "PM 确认工作量")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{workloadId}/confirm")
    public Result<Void> confirm(@PathVariable Long taskId,
                                @PathVariable Long workloadId) {
        workloadService.confirm(workloadId);
        return Result.success();
    }

    @Operation(summary = "PM 驳回工作量")
    @OperationLog(module = AgentConstant.MODULE_WORKLOAD, type = "UPDATE", description = "PM 驳回工作量")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PutMapping("/{workloadId}/reject")
    public Result<Void> reject(@PathVariable Long taskId,
                               @PathVariable Long workloadId,
                               @RequestParam(required = false) String remark) {
        workloadService.reject(workloadId, remark);
        return Result.success();
    }
}
