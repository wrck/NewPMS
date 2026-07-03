package com.vibe.system.controller;

import com.vibe.common.context.UserContextHolder;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.Result;
import com.vibe.annotation.OperationLog;
import com.vibe.system.dto.SysNoticeDTO;
import com.vibe.system.dto.SysNoticeQueryDTO;
import com.vibe.system.service.SysNoticeService;
import com.vibe.system.vo.SysNoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内信 Controller
 *
 * @author vibe
 */
@Tag(name = "站内信", description = "站内信列表、已读标记、未读计数")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService sysNoticeService;

    @Operation(summary = "分页查询当前用户站内信")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public Result<PageResult<SysNoticeVO>> myNotices(@ParameterObject SysNoticeQueryDTO query) {
        return Result.success(sysNoticeService.pageMyNotices(query, UserContextHolder.getUserId()));
    }

    @Operation(summary = "未读计数")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(sysNoticeService.countUnread(UserContextHolder.getUserId()));
    }

    @Operation(summary = "发送站内信")
    @OperationLog(module = "消息中心", type = "INSERT", description = "发送站内信")
    @PreAuthorize("@ss.hasPermi('system:notice') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public Result<Long> send(@Valid @RequestBody SysNoticeDTO dto) {
        return Result.success(sysNoticeService.send(dto));
    }

    @Operation(summary = "标记单条已读")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        sysNoticeService.markRead(id, UserContextHolder.getUserId());
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        sysNoticeService.markAllRead(UserContextHolder.getUserId());
        return Result.success();
    }

    @Operation(summary = "删除站内信")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysNoticeService.delete(id, UserContextHolder.getUserId());
        return Result.success();
    }
}
