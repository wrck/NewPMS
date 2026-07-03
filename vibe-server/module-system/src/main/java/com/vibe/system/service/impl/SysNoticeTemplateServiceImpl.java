package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.converter.SysConverters;
import com.vibe.system.dto.SysNoticeTemplateDTO;
import com.vibe.system.dto.SysNoticeTemplateQueryDTO;
import com.vibe.system.entity.SysNoticeTemplateEntity;
import com.vibe.system.mapper.SysNoticeTemplateMapper;
import com.vibe.system.service.SysNoticeTemplateService;
import com.vibe.system.vo.SysNoticeTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知模板服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysNoticeTemplateServiceImpl implements SysNoticeTemplateService {

    private final SysNoticeTemplateMapper sysNoticeTemplateMapper;

    @Override
    public PageResult<SysNoticeTemplateVO> page(SysNoticeTemplateQueryDTO query) {
        IPage<SysNoticeTemplateVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysNoticeTemplateVO> result = sysNoticeTemplateMapper.selectTemplatePage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysNoticeTemplateDTO dto) {
        checkCodeUnique(dto.getTemplateCode(), null);
        SysNoticeTemplateEntity entity = new SysNoticeTemplateEntity();
        copyDtoToEntity(dto, entity);
        sysNoticeTemplateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysNoticeTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "模板ID不能为空");
        }
        SysNoticeTemplateEntity exist = sysNoticeTemplateMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知模板不存在");
        }
        if (dto.getTemplateCode() != null && !dto.getTemplateCode().equals(exist.getTemplateCode())) {
            checkCodeUnique(dto.getTemplateCode(), dto.getId());
        }
        copyDtoToEntity(dto, exist);
        sysNoticeTemplateMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (sysNoticeTemplateMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知模板不存在");
        }
        sysNoticeTemplateMapper.deleteById(id);
    }

    @Override
    public SysNoticeTemplateVO getDetail(Long id) {
        SysNoticeTemplateEntity entity = sysNoticeTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知模板不存在");
        }
        return SysConverters.toTemplateVo(entity);
    }

    @Override
    public SysNoticeTemplateEntity getByTemplateCode(String templateCode) {
        return sysNoticeTemplateMapper.selectByTemplateCode(templateCode);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysNoticeTemplateEntity> wrapper = new LambdaQueryWrapper<SysNoticeTemplateEntity>()
                .eq(SysNoticeTemplateEntity::getTemplateCode, code);
        if (excludeId != null) {
            wrapper.ne(SysNoticeTemplateEntity::getId, excludeId);
        }
        if (sysNoticeTemplateMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "模板编码已存在");
        }
    }

    private void copyDtoToEntity(SysNoticeTemplateDTO dto, SysNoticeTemplateEntity entity) {
        entity.setTemplateCode(dto.getTemplateCode());
        entity.setTemplateName(dto.getTemplateName());
        entity.setTitleTemplate(dto.getTitleTemplate());
        entity.setContentTemplate(dto.getContentTemplate());
        entity.setChannels(dto.getChannels());
        entity.setRecipientType(dto.getRecipientType());
        entity.setStatus(dto.getStatus() == null ? SystemConstant.STATUS_ENABLED : dto.getStatus());
    }
}
