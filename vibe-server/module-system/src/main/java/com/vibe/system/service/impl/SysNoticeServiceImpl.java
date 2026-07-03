package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.converter.SysConverters;
import com.vibe.system.dto.SysNoticeDTO;
import com.vibe.system.dto.SysNoticeQueryDTO;
import com.vibe.system.entity.SysNoticeEntity;
import com.vibe.system.mapper.SysNoticeMapper;
import com.vibe.system.service.SysNoticeService;
import com.vibe.system.vo.SysNoticeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 站内信服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl implements SysNoticeService {

    private final SysNoticeMapper sysNoticeMapper;

    @Override
    public PageResult<SysNoticeVO> pageMyNotices(SysNoticeQueryDTO query, Long recipientId) {
        IPage<SysNoticeVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysNoticeVO> result = sysNoticeMapper.selectNoticePage(page, query, recipientId);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long send(SysNoticeDTO dto) {
        if (dto.getRecipientId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "接收人ID不能为空");
        }
        SysNoticeEntity entity = new SysNoticeEntity();
        entity.setNoticeTitle(dto.getNoticeTitle());
        entity.setNoticeType(dto.getNoticeType() == null
                ? SystemConstant.NOTICE_TYPE_NOTICE : dto.getNoticeType());
        entity.setNoticeContent(dto.getNoticeContent());
        entity.setRecipientId(dto.getRecipientId());
        entity.setReadStatus(SystemConstant.READ_UNREAD);
        entity.setSendTime(LocalDateTime.now());
        sysNoticeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long noticeId, Long recipientId) {
        // 校验归属，防止越权标记他人站内信
        SysNoticeEntity exist = sysNoticeMapper.selectById(noticeId);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "站内信不存在");
        }
        if (recipientId != null && !recipientId.equals(exist.getRecipientId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "无权操作他人站内信");
        }
        LambdaUpdateWrapper<SysNoticeEntity> update = new LambdaUpdateWrapper<SysNoticeEntity>()
                .eq(SysNoticeEntity::getId, noticeId)
                .eq(SysNoticeEntity::getReadStatus, SystemConstant.READ_UNREAD)
                .set(SysNoticeEntity::getReadStatus, SystemConstant.READ_READ);
        sysNoticeMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long recipientId) {
        LambdaUpdateWrapper<SysNoticeEntity> update = new LambdaUpdateWrapper<SysNoticeEntity>()
                .eq(SysNoticeEntity::getRecipientId, recipientId)
                .eq(SysNoticeEntity::getReadStatus, SystemConstant.READ_UNREAD)
                .set(SysNoticeEntity::getReadStatus, SystemConstant.READ_READ);
        sysNoticeMapper.update(null, update);
    }

    @Override
    public long countUnread(Long recipientId) {
        return sysNoticeMapper.countUnread(recipientId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long noticeId, Long recipientId) {
        LambdaQueryWrapper<SysNoticeEntity> wrapper = new LambdaQueryWrapper<SysNoticeEntity>()
                .eq(SysNoticeEntity::getId, noticeId);
        if (recipientId != null) {
            wrapper.eq(SysNoticeEntity::getRecipientId, recipientId);
        }
        if (sysNoticeMapper.selectCount(wrapper) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "站内信不存在或无权操作");
        }
        sysNoticeMapper.deleteById(noticeId);
    }
}
