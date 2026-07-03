package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysNoticeDTO;
import com.vibe.system.dto.SysNoticeQueryDTO;
import com.vibe.system.vo.SysNoticeVO;

/**
 * 站内信服务
 *
 * @author vibe
 */
public interface SysNoticeService {

    /**
     * 按接收人分页查询站内信
     */
    PageResult<SysNoticeVO> pageMyNotices(SysNoticeQueryDTO query, Long recipientId);

    /**
     * 发送站内信
     */
    Long send(SysNoticeDTO dto);

    /**
     * 标记已读
     */
    void markRead(Long noticeId, Long recipientId);

    /**
     * 全部标记已读
     */
    void markAllRead(Long recipientId);

    /**
     * 未读计数
     */
    long countUnread(Long recipientId);

    /**
     * 删除站内信
     */
    void delete(Long noticeId, Long recipientId);
}
