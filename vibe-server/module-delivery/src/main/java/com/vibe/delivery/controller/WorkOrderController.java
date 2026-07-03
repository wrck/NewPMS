package com.vibe.delivery.controller;

import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.delivery.dto.WorkOrderCheckinDTO;
import com.vibe.delivery.dto.WorkOrderCheckoutDTO;
import com.vibe.delivery.dto.WorkOrderConfirmDTO;
import com.vibe.delivery.dto.WorkOrderCreateDTO;
import com.vibe.delivery.dto.WorkOrderQueryDTO;
import com.vibe.delivery.service.WorkOrderService;
import com.vibe.delivery.vo.WorkOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 工单 Controller（移动端现场作业）
 *
 * <p>路径：{@code /api/v1/work-orders}</p>
 *
 * <p>权限：</p>
 * <ul>
 *   <li>创建工单：PM / 调度员 / 管理员</li>
 *   <li>签到/签退/标记完成：ENGINEER（数据权限仅看自己的工单）</li>
 *   <li>PM 确认：PM</li>
 * </ul>
 *
 * @author vibe
 */
@Tag(name = "工单管理", description = "现场作业工单：创建/签到/签退/完成确认")
@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @Operation(summary = "创建工单")
    @OperationLog(module = "交付管理", type = "INSERT", description = "创建工单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody WorkOrderCreateDTO dto) {
        return Result.success(workOrderService.createWorkOrder(dto));
    }

    @Operation(summary = "分页查询工单列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER')")
    @GetMapping
    public Result<PageResult<WorkOrderVO>> page(@ParameterObject WorkOrderQueryDTO query) {
        return Result.success(workOrderService.page(query));
    }

    @Operation(summary = "工单详情（含步骤/照片/异常）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM','DISPATCHER','ENGINEER')")
    @GetMapping("/{id}")
    public Result<WorkOrderVO> detail(@PathVariable Long id) {
        return Result.success(workOrderService.getDetail(id));
    }

    @Operation(summary = "移动端-我的工单（按状态筛选）")
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER')")
    @GetMapping("/me")
    public Result<List<WorkOrderVO>> myWorkOrders(@RequestParam(required = false) String status) {
        return Result.success(workOrderService.listMyWorkOrders(status));
    }

    @Operation(summary = "现场签到（GPS 定位校验+拍照防作弊）")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "工单签到", saveRequest = false)
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping(value = "/{id}/checkin", consumes = "multipart/form-data")
    public Result<WorkOrderVO> checkin(@PathVariable Long id,
                                       @Valid WorkOrderCheckinDTO dto,
                                       @RequestParam(value = "photo", required = false) MultipartFile photo) {
        return Result.success(workOrderService.checkin(id, dto, photo));
    }

    @Operation(summary = "现场签退")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "工单签退", saveRequest = false)
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping(value = "/{id}/checkout", consumes = "multipart/form-data")
    public Result<WorkOrderVO> checkout(@PathVariable Long id,
                                        @Valid WorkOrderCheckoutDTO dto,
                                        @RequestParam(value = "photo", required = false) MultipartFile photo) {
        return Result.success(workOrderService.checkout(id, dto, photo));
    }

    @Operation(summary = "工程师标记完成（要求所有步骤完成+已签退）")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "工程师标记工单完成")
    @PreAuthorize("hasAnyRole('ENGINEER','AGENT_ENGINEER','SUPER_ADMIN')")
    @PostMapping("/{id}/complete")
    public Result<WorkOrderVO> complete(@PathVariable Long id,
                                        @RequestParam(required = false) String remark) {
        return Result.success(workOrderService.engineerComplete(id, remark));
    }

    @Operation(summary = "PM 确认完成（自动推进项目任务进度）")
    @OperationLog(module = "交付管理", type = "UPDATE", description = "PM 确认工单完成")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DIRECTOR','PM')")
    @PostMapping("/{id}/confirm")
    public Result<WorkOrderVO> confirm(@PathVariable Long id,
                                       @RequestBody(required = false) WorkOrderConfirmDTO dto) {
        return Result.success(workOrderService.pmConfirm(id, dto));
    }
}
