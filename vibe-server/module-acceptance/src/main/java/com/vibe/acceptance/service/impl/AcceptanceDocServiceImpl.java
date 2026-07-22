package com.vibe.acceptance.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.acceptance.dto.AcceptanceDocQueryDTO;
import com.vibe.acceptance.dto.AcceptanceDocSaveDTO;
import com.vibe.acceptance.entity.AcceptanceDocEntity;
import com.vibe.acceptance.mapper.AcceptanceDocMapper;
import com.vibe.acceptance.service.AcceptanceDocService;
import com.vibe.acceptance.vo.AcceptanceDocVO;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 竣工文档 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class AcceptanceDocServiceImpl implements AcceptanceDocService {

    private final AcceptanceDocMapper docMapper;

    @Override
    public PageResult<AcceptanceDocVO> page(AcceptanceDocQueryDTO query) {
        IPage<AcceptanceDocVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<AcceptanceDocVO> result = docMapper.selectDocPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AcceptanceDocVO getDetail(Long id) {
        AcceptanceDocVO vo = docMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.notFound("竣工文档");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(AcceptanceDocSaveDTO dto) {
        AcceptanceDocEntity entity = new AcceptanceDocEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setUploaderId(UserContextHolder.getUserId());
        if (entity.getDocVersion() == null || entity.getDocVersion().isBlank()) {
            entity.setDocVersion("1.0.0");
        }
        docMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AcceptanceDocSaveDTO dto) {
        AcceptanceDocEntity entity = docMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("竣工文档");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        docMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AcceptanceDocEntity entity = docMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("竣工文档");
        }
        docMapper.deleteById(id);
    }
}
