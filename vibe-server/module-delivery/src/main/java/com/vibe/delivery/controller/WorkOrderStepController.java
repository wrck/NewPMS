package com.vibe.delivery.controller;

import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.delivery.dto.WorkOrderStepCompleteDTO;
import com.vibe.delivery.service.WorkOrderStepService;
import com.vibe.delivery.vo.WorkOrderStepVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单施工步骤 Controller
 *
 * <p>路径：{@code /api/v1/work-orders/{workOrderId}/steps}</p>
 *
 * @author vibe
 */
@Tag(name = "施工步骤", description = "工单施工步骤跟踪：列表/标记完成")
@RestController
@RequestMapping("/api/v1/work-orders/{workOrderId}/steps")
@RequiredArgsConstructor
public class WorkOrderStepController {

    private final WorkOrderStepService workOrderStepService;

    @Operation(summary = "查询工单施工步骤列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER','AGENT_ENGINEER')")
    @GetMapping
    public Result<List<WorkOrderStepVO>> list(@PathVariable Long workOrderId) {
        return Result.success(workOrderStepService.listByWorkOrder(workOrderId));
    }

    @Operation(summary = "标记步骤完成/跳过")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "标记施工步骤完成")
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping("/{stepId}/complete")
    public Result<WorkOrderStepVO> complete(@PathVariable Long workOrderId,
                                            @PathVariable Long stepId,
                                            @RequestBody(required = false) WorkOrderStepCompleteDTO dto) {
        return Result.success(workOrderStepService.completeStep(workOrderId, stepId, dto));
    }
}
