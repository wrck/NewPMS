package com.vibe.delivery.controller;

import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.delivery.dto.WorkOrderIssueProcessDTO;
import com.vibe.delivery.dto.WorkOrderIssueReportDTO;
import com.vibe.delivery.service.WorkOrderIssueService;
import com.vibe.delivery.vo.WorkOrderIssueVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单异常问题 Controller
 *
 * <p>路径：{@code /api/v1/work-orders/{workOrderId}/issues}</p>
 *
 * @author vibe
 */
@Tag(name = "异常上报", description = "工单异常问题上报/处理跟踪")
@RestController
@RequestMapping("/api/v1/work-orders/{workOrderId}/issues")
@RequiredArgsConstructor
public class WorkOrderIssueController {

    private final WorkOrderIssueService workOrderIssueService;

    @Operation(summary = "上报异常问题（自动通知 PM）")
    @OperationLog(module = "交付管理", type = "INSERT", description = "上报异常问题")
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping
    public Result<Long> report(@PathVariable Long workOrderId,
                               @Valid @RequestBody WorkOrderIssueReportDTO dto) {
        return Result.success(workOrderIssueService.reportIssue(workOrderId, dto));
    }

    @Operation(summary = "查询工单异常问题列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER','AGENT_ENGINEER')")
    @GetMapping
    public Result<List<WorkOrderIssueVO>> list(@PathVariable Long workOrderId) {
        return Result.success(workOrderIssueService.listByWorkOrder(workOrderId));
    }

    @Operation(summary = "异常问题详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER','AGENT_ENGINEER')")
    @GetMapping("/{issueId}")
    public Result<WorkOrderIssueVO> detail(@PathVariable Long workOrderId,
                                           @PathVariable Long issueId) {
        return Result.success(workOrderIssueService.getDetail(issueId));
    }

    @Operation(summary = "处理异常问题（状态流转 OPEN→PROCESSING→RESOLVED→CLOSED）")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "处理异常问题")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER')")
    @PutMapping("/{issueId}/process")
    public Result<WorkOrderIssueVO> process(@PathVariable Long workOrderId,
                                            @PathVariable Long issueId,
                                            @Valid @RequestBody WorkOrderIssueProcessDTO dto) {
        return Result.success(workOrderIssueService.process(issueId, dto));
    }
}
