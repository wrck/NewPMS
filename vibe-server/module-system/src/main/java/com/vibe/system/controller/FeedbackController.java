package com.vibe.system.controller;

import com.vibe.common.context.UserContextHolder;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysFeedbackDTO;
import com.vibe.system.dto.SysFeedbackHandleDTO;
import com.vibe.system.dto.SysFeedbackQueryDTO;
import com.vibe.system.service.SysFeedbackService;
import com.vibe.system.vo.SysFeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反馈与工单 Controller
 *
 * <p>权限策略：</p>
 * <ul>
 *   <li>提交反馈：任意已登录用户（无特殊权限要求）</li>
 *   <li>查看本人反馈：任意已登录用户</li>
 *   <li>管理（分页查询全部 / 处理反馈）：需要 system:feedback:list 权限或 SUPER_ADMIN</li>
 * </ul>
 *
 * @author vibe
 */
@Tag(name = "反馈与工单", description = "用户反馈收集、状态处理与通知")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final SysFeedbackService sysFeedbackService;

    @Operation(summary = "提交反馈")
    @OperationLog(module = "反馈中心", type = "INSERT", description = "提交反馈")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public Result<Long> submit(@Valid @RequestBody SysFeedbackDTO dto) {
        return Result.success(sysFeedbackService.submit(dto, UserContextHolder.getUserId()));
    }

    @Operation(summary = "管理员分页查询全部反馈")
    @OperationLog(module = "反馈中心", type = "QUERY", description = "分页查询反馈列表")
    @PreAuthorize("@ss.hasPermi('system:feedback:list') or hasRole('SUPER_ADMIN')")
    @GetMapping
    public Result<PageResult<SysFeedbackVO>> pageAll(@ParameterObject SysFeedbackQueryDTO query) {
        return Result.success(sysFeedbackService.pageAll(query));
    }

    @Operation(summary = "查询我提交的反馈")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    public Result<PageResult<SysFeedbackVO>> pageMine(@ParameterObject SysFeedbackQueryDTO query) {
        return Result.success(sysFeedbackService.pageMy(query, UserContextHolder.getUserId()));
    }

    @Operation(summary = "处理反馈（状态变更）")
    @OperationLog(module = "反馈中心", type = "UPDATE", description = "处理反馈")
    @PreAuthorize("@ss.hasPermi('system:feedback:handle') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody SysFeedbackHandleDTO dto) {
        sysFeedbackService.handle(id, dto, UserContextHolder.getUserId());
        return Result.success();
    }
}
