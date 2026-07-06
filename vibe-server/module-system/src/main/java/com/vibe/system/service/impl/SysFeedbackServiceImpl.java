package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysFeedbackDTO;
import com.vibe.system.dto.SysFeedbackHandleDTO;
import com.vibe.system.dto.SysFeedbackQueryDTO;
import com.vibe.system.dto.SysNoticeDTO;
import com.vibe.system.entity.SysFeedbackEntity;
import com.vibe.system.mapper.SysFeedbackMapper;
import com.vibe.system.service.SysFeedbackService;
import com.vibe.system.service.SysNoticeService;
import com.vibe.system.vo.SysFeedbackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 反馈与工单服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFeedbackServiceImpl implements SysFeedbackService {

    private final SysFeedbackMapper sysFeedbackMapper;
    /** 用于反馈状态变更时通知提交人（站内信） */
    private final SysNoticeService sysNoticeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(SysFeedbackDTO dto, Long submitterId) {
        if (submitterId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录用户不能提交反馈");
        }
        SysFeedbackEntity entity = new SysFeedbackEntity();
        entity.setType(dto.getType());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setScreenshotUrl(dto.getScreenshotUrl());
        entity.setContact(dto.getContact());
        entity.setSubmitterId(submitterId);
        entity.setStatus(SystemConstant.FEEDBACK_STATUS_PENDING);
        sysFeedbackMapper.insert(entity);
        log.info("[feedback] 用户 {} 提交反馈 id={} type={} title={}",
                submitterId, entity.getId(), entity.getType(), entity.getTitle());
        return entity.getId();
    }

    @Override
    public PageResult<SysFeedbackVO> pageAll(SysFeedbackQueryDTO query) {
        IPage<SysFeedbackVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysFeedbackVO> result = sysFeedbackMapper.selectFeedbackPage(page, query, null);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<SysFeedbackVO> pageMy(SysFeedbackQueryDTO query, Long submitterId) {
        IPage<SysFeedbackVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysFeedbackVO> result = sysFeedbackMapper.selectFeedbackPage(page, query, submitterId);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long feedbackId, SysFeedbackHandleDTO dto, Long handlerId) {
        SysFeedbackEntity exist = sysFeedbackMapper.selectById(feedbackId);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "反馈不存在");
        }
        exist.setStatus(dto.getStatus());
        exist.setHandlerId(handlerId);
        exist.setHandleNote(dto.getHandleNote());
        exist.setHandleTime(LocalDateTime.now());
        sysFeedbackMapper.updateById(exist);
        log.info("[feedback] 反馈 {} 状态变更为 {} 处理人 {}", feedbackId, dto.getStatus(), handlerId);

        // 通知提交人（站内信）
        try {
            SysNoticeDTO notice = new SysNoticeDTO();
            notice.setNoticeTitle("反馈处理进度更新：「" + exist.getTitle() + "」");
            notice.setNoticeType(SystemConstant.NOTICE_TYPE_MSG);
            notice.setNoticeContent(buildNoticeContent(exist.getTitle(), dto.getStatus(), dto.getHandleNote()));
            notice.setRecipientId(exist.getSubmitterId());
            sysNoticeService.send(notice);
        } catch (Exception e) {
            // 站内信发送失败不影响主流程
            log.warn("[feedback] 通知提交人失败 feedbackId={}: {}", feedbackId, e.getMessage());
        }
    }

    private String buildNoticeContent(String title, String newStatus, String note) {
        String statusLabel = switch (newStatus) {
            case "PROCESSING" -> "处理中";
            case "RESOLVED" -> "已解决";
            case "CLOSED" -> "已关闭";
            default -> newStatus;
        };
        StringBuilder sb = new StringBuilder();
        sb.append("您的反馈「").append(title).append("」状态已更新为：").append(statusLabel).append("。");
        if (note != null && !note.isEmpty()) {
            sb.append("\n处理备注：").append(note);
        }
        return sb.toString();
    }
}
