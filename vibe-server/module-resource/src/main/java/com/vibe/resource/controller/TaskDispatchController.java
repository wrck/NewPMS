package com.vibe.resource.controller;

import com.vibe.annotation.OperationLog;
import com.vibe.common.result.Result;
import com.vibe.resource.constant.ResourceConstant;
import com.vibe.resource.dto.BatchDispatchDTO;
import com.vibe.resource.dto.TaskDispatchDTO;
import com.vibe.resource.dto.TaskRecommendationQueryDTO;
import com.vibe.resource.dto.TaskReassignDTO;
import com.vibe.resource.dto.TaskReturnDTO;
import com.vibe.resource.service.TaskDispatchService;
import com.vibe.resource.vo.DispatchResultVO;
import com.vibe.resource.vo.EngineerRecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 任务派发 Controller
 *
 * <p>路径前缀 {@code /api/v1/dispatches}。提供手动指派、批量派单、智能推荐、
 * 转派、退回、紧急调配能力。写操作限 SUPER_ADMIN / DISPATCHER / PM。</p>
 *
 * @author vibe
 */
@Tag(name = "任务派发", description = "手动指派/批量派单/智能推荐/转派/退回/紧急调配")
@RestController
@RequestMapping("/api/v1/dispatches")
@RequiredArgsConstructor
public class TaskDispatchController {

    private final TaskDispatchService taskDispatchService;

    @Operation(summary = "手动指派：指定工程师，写排期并更新 project_task.assignee_id")
    @OperationLog(module = "任务派发", type = ResourceConstant.BIZ_DISPATCH,
            description = "手动指派", saveResponse = true)
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @PostMapping
    public Result<Long> dispatch(@Valid @RequestBody TaskDispatchDTO dto) {
        return Result.success(taskDispatchService.dispatch(dto));
    }

    @Operation(summary = "批量派单：返回成功/失败明细")
    @OperationLog(module = "任务派发", type = ResourceConstant.BIZ_DISPATCH, description = "批量派单")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @PostMapping("/batch")
    public Result<DispatchResultVO> batchDispatch(@Valid @RequestBody BatchDispatchDTO dto) {
        return Result.success(taskDispatchService.batchDispatch(dto));
    }

    @Operation(summary = "智能推荐：基于技能匹配/区域就近/当前负荷返回推荐工程师列表")
    @OperationLog(module = "任务派发", type = ResourceConstant.BIZ_QUERY, description = "智能推荐工程师")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @PostMapping("/recommend")
    public Result<List<EngineerRecommendationVO>> recommend(@Valid @RequestBody TaskRecommendationQueryDTO query) {
        return Result.success(taskDispatchService.recommend(query));
    }

    @Operation(summary = "转派：释放原工程师排期，分配新工程师")
    @OperationLog(module = "任务派发", type = ResourceConstant.BIZ_DISPATCH, description = "任务转派")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @PostMapping("/reassign")
    public Result<Long> reassign(@Valid @RequestBody TaskReassignDTO dto) {
        return Result.success(taskDispatchService.reassign(dto));
    }

    @Operation(summary = "退回：释放排期，任务回到待分配（PENDING）")
    @OperationLog(module = "任务派发", type = ResourceConstant.BIZ_DISPATCH, description = "任务退回")
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER
            + "') or hasRole('" + ResourceConstant.ROLE_PM + "')")
    @PostMapping("/return")
    public Result<Void> returnTask(@Valid @RequestBody TaskReturnDTO dto) {
        taskDispatchService.returnTask(dto);
        return Result.success();
    }

    @Operation(summary = "紧急调配：跳过冲突检测强制指派（管理层干预）")
    @OperationLog(module = "任务派发", type = ResourceConstant.BIZ_DISPATCH,
            description = "紧急调配", saveResponse = true)
    @PreAuthorize("hasRole('" + ResourceConstant.ROLE_SUPER_ADMIN
            + "') or hasRole('" + ResourceConstant.ROLE_DISPATCHER + "')")
    @PostMapping("/urgent")
    public Result<Long> urgentDispatch(@Valid @RequestBody TaskDispatchDTO dto) {
        return Result.success(taskDispatchService.urgentDispatch(dto));
    }
}
