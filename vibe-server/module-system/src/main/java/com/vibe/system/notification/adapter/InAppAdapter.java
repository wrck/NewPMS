package com.vibe.system.notification.adapter;

import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysNoticeDTO;
import com.vibe.system.service.SysNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内信渠道适配器
 *
 * <p>调用 {@link SysNoticeService#send} 写入 sys_notice 表，
 * 接收人通过站内消息中心查看。</p>
 *
 * @author vibe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppAdapter {

    private final SysNoticeService sysNoticeService;

    /**
     * 写入站内信给指定接收人。
     *
     * @param recipientId 接收人用户 ID
     * @param title       通知标题
     * @param content     通知内容
     */
    public void send(Long recipientId, String title, String content) {
        if (recipientId == null) {
            log.warn("站内信发送跳过：recipientId 为空, title={}", title);
            return;
        }
        try {
            SysNoticeDTO dto = new SysNoticeDTO();
            dto.setNoticeTitle(title == null ? "" : title);
            dto.setNoticeContent(content == null ? "" : content);
            dto.setNoticeType(SystemConstant.NOTICE_TYPE_NOTICE);
            dto.setRecipientId(recipientId);
            Long noticeId = sysNoticeService.send(dto);
            log.info("站内信写入成功: noticeId={}, recipientId={}, title={}", noticeId, recipientId, title);
        } catch (Exception e) {
            log.error("站内信写入失败: recipientId={}, title={}, error={}",
                    recipientId, title, e.getMessage(), e);
        }
    }
}
